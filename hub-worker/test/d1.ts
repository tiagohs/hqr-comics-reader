// A small D1 stand-in over node:sqlite, enough for the Worker's queries.
import { DatabaseSync } from "node:sqlite";
import { readFileSync, readdirSync } from "node:fs";
import { join } from "node:path";

class Statement {
  constructor(
    private readonly db: DatabaseSync,
    private readonly sql: string,
    private readonly params: unknown[] = [],
  ) {}
  bind(...params: unknown[]) {
    return new Statement(this.db, this.sql, params);
  }
  async first<T>() {
    return (this.db.prepare(this.sql).get(...(this.params as never[])) as T) ?? null;
  }
  async all<T>() {
    return { results: this.db.prepare(this.sql).all(...(this.params as never[])) as T[] };
  }
  async run() {
    const info = this.db.prepare(this.sql).run(...(this.params as never[]));
    return { meta: { changes: Number(info.changes) } };
  }
  runSync() {
    this.db.prepare(this.sql).run(...(this.params as never[]));
  }
}

export interface TestDb {
  db: D1Database;
  raw: DatabaseSync;
  reads: () => number;
}

export function testDb(): TestDb {
  const raw = new DatabaseSync(":memory:");
  const dir = join(__dirname, "..", "migrations");
  for (const file of readdirSync(dir).sort()) raw.exec(readFileSync(join(dir, file), "utf8"));
  let rowsRead = 0;
  const db = {
    prepare(sql: string) {
      const statement = new Statement(raw, sql);
      const wrap = (s: Statement): unknown => ({
        bind: (...p: unknown[]) => wrap(s.bind(...p)),
        first: async () => {
          const row = await s.first();
          if (row) rowsRead += 1;
          return row;
        },
        all: async () => {
          const rows = await s.all();
          rowsRead += rows.results.length;
          return rows;
        },
        run: () => s.run(),
        runSync: () => s.runSync(),
      });
      return wrap(statement);
    },
    async batch(statements: { runSync: () => void }[]) {
      raw.exec("BEGIN");
      try {
        for (const statement of statements) statement.runSync();
        raw.exec("COMMIT");
      } catch (error) {
        raw.exec("ROLLBACK");
        throw error;
      }
      return [];
    },
  } as unknown as D1Database;
  return { db, raw, reads: () => rowsRead };
}
