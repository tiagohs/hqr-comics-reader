// POST /api/v1/sync: stores the device's changes and returns what changed since its cursor (ADR-029).
// The Worker only keeps the latest version of each item and orders them; merge rules live in the app.

export const SYNC_KINDS = ["manga", "chapter", "category", "saved_search", "feed", "server", "setting"] as const;
export const MAX_CHANGES_IN = 1000;
export const PAGE_SIZE = 500;
const MAX_KEY = 512;
const MAX_PAYLOAD = 64 * 1024;

export interface Change {
  kind: string;
  key: string;
  payload: string;
  changedAt: number;
  deleted: boolean;
}

export interface SyncRequest {
  cursor: number;
  changes: Change[];
}

export interface SyncResponse {
  cursor: number;
  changes: (Change & { seq: number })[];
  more: boolean;
}

export function parseSyncRequest(body: unknown): SyncRequest | null {
  if (typeof body !== "object" || body === null) return null;
  const { cursor, changes } = body as { cursor?: unknown; changes?: unknown };
  if (!Number.isInteger(cursor) || (cursor as number) < 0) return null;
  if (!Array.isArray(changes) || changes.length > MAX_CHANGES_IN) return null;
  const parsed: Change[] = [];
  for (const raw of changes) {
    if (typeof raw !== "object" || raw === null) return null;
    const item = raw as Record<string, unknown>;
    if (
      !SYNC_KINDS.includes(item.kind as (typeof SYNC_KINDS)[number]) ||
      typeof item.key !== "string" ||
      item.key === "" ||
      item.key.length > MAX_KEY ||
      typeof item.payload !== "string" ||
      item.payload.length > MAX_PAYLOAD ||
      !Number.isInteger(item.changedAt) ||
      typeof item.deleted !== "boolean"
    ) {
      return null;
    }
    parsed.push({
      kind: item.kind as string,
      key: item.key,
      payload: item.payload,
      changedAt: item.changedAt as number,
      deleted: item.deleted,
    });
  }
  return { cursor: cursor as number, changes: parsed };
}

/**
 * Applies the incoming changes (an older change than the stored one is ignored) and reads the
 * page after the cursor. A cycle without changes reads the account row and nothing else.
 */
export async function sync(db: D1Database, accountId: number, request: SyncRequest): Promise<SyncResponse> {
  const accepted: Change[] = [];
  if (request.changes.length > 0) {
    const keys = request.changes.map((change) => [change.kind, change.key]);
    const stored = new Map<string, number>();
    // Reads the stored versions in chunks to stay under D1's bound-parameter limit.
    for (let start = 0; start < keys.length; start += 40) {
      const chunk = keys.slice(start, start + 40);
      const where = chunk.map(() => "(kind = ? AND key = ?)").join(" OR ");
      const rows = await db
        .prepare(`SELECT kind, key, changedAt FROM record WHERE accountId = ? AND (${where})`)
        .bind(accountId, ...chunk.flat())
        .all<{ kind: string; key: string; changedAt: number }>();
      for (const row of rows.results) stored.set(`${row.kind}\u0000${row.key}`, row.changedAt);
    }
    const latest = new Map<string, Change>();
    for (const change of request.changes) {
      const id = `${change.kind}\u0000${change.key}`;
      const previous = latest.get(id);
      if (previous && previous.changedAt > change.changedAt) continue;
      const current = stored.get(id);
      if (current !== undefined && current > change.changedAt) continue;
      latest.set(id, change);
    }
    accepted.push(...latest.values());
  }

  if (accepted.length > 0) {
    const account = await db
      .prepare("UPDATE account SET seq = seq + ? WHERE id = ? RETURNING seq")
      .bind(accepted.length, accountId)
      .first<{ seq: number }>();
    if (!account) throw new Error("account");
    const first = account.seq - accepted.length + 1;
    await db.batch(
      accepted.map((change, index) =>
        db
          .prepare(
            "INSERT INTO record (accountId, kind, key, payload, changedAt, deleted, seq) VALUES (?, ?, ?, ?, ?, ?, ?) " +
              "ON CONFLICT(accountId, kind, key) DO UPDATE SET payload = excluded.payload, " +
              "changedAt = excluded.changedAt, deleted = excluded.deleted, seq = excluded.seq",
          )
          .bind(accountId, change.kind, change.key, change.payload, change.changedAt, change.deleted ? 1 : 0, first + index),
      ),
    );
  }

  const head = await db.prepare("SELECT seq FROM account WHERE id = ?").bind(accountId).first<{ seq: number }>();
  const top = head?.seq ?? 0;
  if (top <= request.cursor) return { cursor: top, changes: [], more: false };

  const rows = await db
    .prepare(
      "SELECT kind, key, payload, changedAt, deleted, seq FROM record WHERE accountId = ? AND seq > ? ORDER BY seq LIMIT ?",
    )
    .bind(accountId, request.cursor, PAGE_SIZE + 1)
    .all<{ kind: string; key: string; payload: string; changedAt: number; deleted: number; seq: number }>();
  const more = rows.results.length > PAGE_SIZE;
  const page = rows.results.slice(0, PAGE_SIZE).map((row) => ({ ...row, deleted: row.deleted === 1 }));
  return { cursor: more ? page[page.length - 1].seq : top, changes: page, more };
}
