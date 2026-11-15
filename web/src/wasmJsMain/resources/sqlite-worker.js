// SQLite worker for Room in the browser (ADR-004): speaks the androidx.sqlite web protocol and stores the
// database in the Origin Private File System through the SAH pool (no special headers needed on GitHub Pages).
import sqlite3InitModule from './sqlite3.mjs';

let sqlite3;
let pool = null;
const dbs = new Map();
const stmts = new Map();
let nextDb = 1;
let nextStmt = 1;

// Only one tab at a time can open the stored library. A lock held for this worker's life waits for a tab that is
// closing (a reload, the Google sign-in coming back) instead of falling straight back to memory.
const LOCK_WAIT_MS = 3000;

function holdLibraryLock() {
  if (!self.navigator || !navigator.locks) return Promise.resolve(true);
  return new Promise((resolve) => {
    navigator.locks
      .request('hqr-library', { signal: AbortSignal.timeout(LOCK_WAIT_MS) }, () => {
        resolve(true);
        return new Promise(() => {});
      })
      .catch(() => resolve(false));
  });
}

const ready = sqlite3InitModule().then(async (s) => {
  sqlite3 = s;
  try {
    if (!(await holdLibraryLock())) throw new Error('the library is open in another tab');
    pool = await s.installOpfsSAHPoolVfs({ name: 'hqr-pool' });
    console.info('HQR: library stored in OPFS');
  } catch (e) {
    console.warn('HQR: OPFS unavailable, the library lives in memory for this tab', e);
    // Without OPFS (private window, old browser, another tab open) the data lives in memory for this tab only; signed
    // in, the sync still brings the library (the cursor lives in the database, so a fresh one starts from zero).
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
  // getColumnNames() throws for statements without columns (PRAGMA x = y, INSERT…).
  const columnNames = [];
  for (let c = 0; c < st.columnCount; c++) columnNames.push(st.getColumnName(c));
  return { statementId: id, parameterCount: st.parameterCount, columnNames };
}

function step(statementId, bindings) {
  const st = stmts.get(statementId);
  st.reset(true);
  if (bindings) {
    // androidx.sqlite 2.7 sends the bindings 0-based (bindX(index) stores at index - 1); SQLite counts from 1.
    for (let i = 0; i < bindings.length; i++) {
      const v = bindings[i];
      st.bind(i + 1, v === undefined ? null : v);
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
  // Room reads column types even when no row came back: every column is then NULL (5).
  if (first) {
    for (let c = 0; c < st.columnCount; c++) columnTypes.push(5);
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
