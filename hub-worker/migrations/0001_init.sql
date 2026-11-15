-- Accounts, sessions and synced records (ADR-027 to ADR-029).
CREATE TABLE account (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  email TEXT NOT NULL UNIQUE,
  seq INTEGER NOT NULL DEFAULT 0,
  createdAt INTEGER NOT NULL
);

CREATE TABLE session (
  tokenHash TEXT PRIMARY KEY,
  accountId INTEGER NOT NULL REFERENCES account(id) ON DELETE CASCADE,
  device TEXT NOT NULL,
  expiresAt INTEGER NOT NULL,
  lastUsedAt INTEGER NOT NULL
);
CREATE INDEX session_account ON session(accountId);

CREATE TABLE record (
  accountId INTEGER NOT NULL REFERENCES account(id) ON DELETE CASCADE,
  kind TEXT NOT NULL,
  key TEXT NOT NULL,
  payload TEXT NOT NULL,
  changedAt INTEGER NOT NULL,
  deleted INTEGER NOT NULL DEFAULT 0,
  seq INTEGER NOT NULL,
  PRIMARY KEY (accountId, kind, key)
);
CREATE INDEX record_seq ON record(accountId, seq);
