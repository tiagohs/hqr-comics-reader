// SQLite worker for Room in the browser (ADR-004): speaks the androidx.sqlite web protocol and stores the
// database in the Origin Private File System through the SAH pool (no special headers needed on GitHub Pages).
import sqlite3InitModule from './sqlite3.mjs';

let sqlite3;
let pool = null;
const dbs = new Map();
const stmts = new Map();
let nextDb = 1;
let nextStmt = 1;

const ready = sqlite3InitModule().then(async (s) => {
  sqlite3 = s;
  try {
    pool = await s.installOpfsSAHPoolVfs({ name: 'hqr-pool' });
  } catch (e) {
    // Without OPFS (private window, old browser) the data lives in memory for this tab only.
    pool = null;
  }
});

function open(fileName) {
  const db = pool ? new pool.OpfsSAHPoolDb('/' + fileName) : new sqlite3.oo1.DB(':memory:', 'c');
  const id = nextDb++;
  dbs.set(id, db);
  return { databaseId: id };
}

function prepare(databaseId, sql) {
  const st = dbs.get(databaseId).prepare(sql);
  const id = nextStmt++;
  stmts.set(id, st);
  return { statementId: id, parameterCount: st.parameterCount, columnNames: st.getColumnNames() };
}

function step(statementId, bindings) {
  const st = stmts.get(statementId);
  st.reset(true);
  if (bindings) {
    // Room binds 1-based; the array may come 0-based (index - 1) or 1-based with an empty slot 0. Accept both.
    const offset = bindings.length === st.parameterCount + 1 && bindings[0] === undefined ? 0 : 1;
    for (let i = 0; i < bindings.length; i++) {
      if (offset === 0 && i === 0) continue;
      const v = bindings[i];
      st.bind(i + offset, v === undefined ? null : v);
    }
  }
  const rows = [];
  const columnTypes = [];
  let first = true;
  while (st.step()) {
    const count = st.columnCount;
    if (first) {
      for (let c = 0; c < count; c++) columnTypes.push(sqlite3.capi.sqlite3_column_type(st.pointer, c));
      first = false;
    }
    const row = [];
    for (let c = 0; c < count; c++) row.push(st.get(c));
    rows.push(row);
  }
  return { rows, columnTypes };
}

function close(statementId, databaseId) {
  // A statement close may carry its database id too: then only the statement closes.
  if (statementId != null) {
    if (stmts.has(statementId)) {
      stmts.get(statementId).finalize();
      stmts.delete(statementId);
    }
    return;
  }
  if (databaseId != null && dbs.has(databaseId)) {
    dbs.get(databaseId).close();
    dbs.delete(databaseId);
  }
}

onmessage = async (event) => {
  const { id, data } = event.data;
  try {
    await ready;
    let result;
    switch (data.cmd) {
      case 'open': result = open(data.fileName); break;
      case 'prepare': result = prepare(data.databaseId, data.sql); break;
      case 'step': result = step(data.statementId, data.bindings); break;
      case 'close': close(data.statementId, data.databaseId); return;
      default: throw new Error('Unknown command ' + data.cmd);
    }
    postMessage({ id, data: result });
  } catch (e) {
    postMessage({ id, error: String(e && e.message ? e.message : e) });
  }
};
