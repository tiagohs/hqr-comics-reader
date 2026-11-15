// HQR sessions: 32 random bytes handed to the app, only the SHA-256 hash kept in D1 (RNF-34).

export const SESSION_DAYS = 90;
const DAY_MS = 86_400_000;
/** A session is pushed forward at most once a day, so a sync without changes writes nothing most of the time. */
const TOUCH_EVERY_MS = DAY_MS;

export interface Session {
  accountId: number;
  email: string;
  tokenHash: string;
}

function toBase64Url(bytes: Uint8Array): string {
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

export function newSessionToken(): string {
  return toBase64Url(crypto.getRandomValues(new Uint8Array(32)));
}

export async function hashToken(token: string): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(token));
  return [...new Uint8Array(digest)].map((byte) => byte.toString(16).padStart(2, "0")).join("");
}

export function bearer(request: Request): string | null {
  const value = request.headers.get("Authorization") ?? "";
  const match = /^Bearer\s+([A-Za-z0-9_-]{20,})$/.exec(value.trim());
  return match ? match[1] : null;
}

export async function createSession(
  db: D1Database,
  email: string,
  device: string,
  now: number,
): Promise<{ token: string; expiresAt: number }> {
  await db
    .prepare("INSERT INTO account (email, seq, createdAt) VALUES (?, 0, ?) ON CONFLICT(email) DO NOTHING")
    .bind(email, now)
    .run();
  const account = await db.prepare("SELECT id FROM account WHERE email = ?").bind(email).first<{ id: number }>();
  if (!account) throw new Error("account");
  const token = newSessionToken();
  const expiresAt = now + SESSION_DAYS * DAY_MS;
  await db
    .prepare("INSERT INTO session (tokenHash, accountId, device, expiresAt, lastUsedAt) VALUES (?, ?, ?, ?, ?)")
    .bind(await hashToken(token), account.id, device.slice(0, 80), expiresAt, now)
    .run();
  return { token, expiresAt };
}

/** The session behind the bearer token, renewed to 90 days with use; null when missing or expired. */
export async function findSession(db: D1Database, request: Request, now: number): Promise<Session | null> {
  const token = bearer(request);
  if (!token) return null;
  const tokenHash = await hashToken(token);
  const row = await db
    .prepare(
      "SELECT s.accountId AS accountId, s.expiresAt AS expiresAt, s.lastUsedAt AS lastUsedAt, a.email AS email " +
        "FROM session s JOIN account a ON a.id = s.accountId WHERE s.tokenHash = ?",
    )
    .bind(tokenHash)
    .first<{ accountId: number; expiresAt: number; lastUsedAt: number; email: string }>();
  if (!row) return null;
  if (row.expiresAt <= now) {
    await db.prepare("DELETE FROM session WHERE tokenHash = ?").bind(tokenHash).run();
    return null;
  }
  if (now - row.lastUsedAt >= TOUCH_EVERY_MS) {
    await db
      .prepare("UPDATE session SET lastUsedAt = ?, expiresAt = ? WHERE tokenHash = ?")
      .bind(now, now + SESSION_DAYS * DAY_MS, tokenHash)
      .run();
  }
  return { accountId: row.accountId, email: row.email, tokenHash };
}

export async function deleteSession(db: D1Database, tokenHash: string): Promise<void> {
  await db.prepare("DELETE FROM session WHERE tokenHash = ?").bind(tokenHash).run();
}
