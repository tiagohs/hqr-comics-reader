// HQR personal Worker (ADR-027): Google sign-in, sync, the bridge to the online sources and the web app.

import { handleBridgeFetch } from "./bridge";
import { type DriveEnv, handleDrive } from "./drive";
import { googleKeys, type KeySource, verifyGoogleIdToken } from "./google";
import { createSession, deleteSession, findSession } from "./session";
import { parseSyncRequest, sync } from "./sync";

export const HUB_VERSION = 1;
const MAX_BODY_BYTES = 4 * 1024 * 1024;

export interface Env extends DriveEnv {
  DB: D1Database;
  ASSETS?: Fetcher;
  /** Comma-separated emails allowed to sign in (secret). */
  ALLOWED_EMAILS?: string;
  /** Comma-separated OAuth client IDs whose tokens are accepted; the web client first (secret). */
  GOOGLE_CLIENT_IDS?: string;
  /** Hosts the bridge may reach; the built-in list when empty. */
  HOSTS?: string;
}

export interface Deps {
  fetch: typeof fetch;
  keys: KeySource;
  now: () => number;
}

export function list(value: string | undefined): string[] {
  return (value ?? "")
    .split(",")
    .map((item) => item.trim())
    .filter((item) => item !== "");
}

export function json(status: number, payload: unknown): Response {
  return new Response(JSON.stringify(payload), {
    status,
    headers: { "Content-Type": "application/json", "Cache-Control": "no-store" },
  });
}

async function readJson(request: Request): Promise<unknown> {
  if (Number(request.headers.get("content-length") ?? 0) > MAX_BODY_BYTES) return undefined;
  try {
    return await request.json();
  } catch {
    return undefined;
  }
}

async function signIn(request: Request, env: Env, deps: Deps): Promise<Response> {
  const body = (await readJson(request)) as { idToken?: unknown; device?: unknown } | undefined;
  if (!body || typeof body.idToken !== "string") return json(400, { error: "body" });
  const audiences = list(env.GOOGLE_CLIENT_IDS);
  const allowed = list(env.ALLOWED_EMAILS).map((email) => email.toLowerCase());
  // Without the secrets the Worker accepts nobody.
  if (audiences.length === 0 || allowed.length === 0) return json(503, { error: "not_configured" });

  let result;
  try {
    result = await verifyGoogleIdToken(body.idToken, audiences, deps.keys, Math.floor(deps.now() / 1000));
  } catch {
    return json(502, { error: "google" });
  }
  if ("error" in result) return json(401, { error: "invalid_token" });
  if (!allowed.includes(result.identity.email)) return json(403, { error: "not_allowed" });

  const device = typeof body.device === "string" && body.device !== "" ? body.device : "unknown";
  const session = await createSession(env.DB, result.identity.email, device, deps.now());
  return json(200, { token: session.token, email: result.identity.email, expiresAt: session.expiresAt });
}

export async function handle(request: Request, env: Env, deps: Deps): Promise<Response> {
  const url = new URL(request.url);
  const path = url.pathname;
  const isApi = path.startsWith("/api/");
  const isBridge = path.startsWith("/bridge/");
  if (!isApi && !isBridge) {
    return env.ASSETS ? env.ASSETS.fetch(request) : json(404, { error: "path" });
  }

  if (path === "/api/v1/health") return json(200, { ok: true, version: HUB_VERSION });
  if (path === "/api/v1/config" && request.method === "GET") {
    return json(200, { googleClientId: list(env.GOOGLE_CLIENT_IDS)[0] ?? null });
  }
  if (path === "/api/v1/auth/google") {
    return request.method === "POST" ? signIn(request, env, deps) : json(405, { error: "method" });
  }

  const session = await findSession(env.DB, request, deps.now());
  if (!session) return json(401, { error: "session" });

  if (isBridge) {
    if (path === "/bridge/health") return json(200, { ok: true, version: HUB_VERSION });
    if (path === "/bridge/fetch") return handleBridgeFetch(request, env.HOSTS, deps.fetch, json);
    return json(404, { error: "path" });
  }
  if (path.startsWith("/api/v1/drive/")) return handleDrive(request, env, deps.fetch, deps.now, json);
  switch (path) {
    case "/api/v1/me":
      return json(200, { email: session.email });
    case "/api/v1/auth/signout":
      if (request.method !== "POST") return json(405, { error: "method" });
      await deleteSession(env.DB, session.tokenHash);
      return json(200, { ok: true });
    case "/api/v1/sync": {
      if (request.method !== "POST") return json(405, { error: "method" });
      const parsed = parseSyncRequest(await readJson(request));
      if (!parsed) return json(400, { error: "body" });
      return json(200, await sync(env.DB, session.accountId, parsed));
    }
    default:
      return json(404, { error: "path" });
  }
}

export default {
  fetch(request: Request, env: Env): Promise<Response> {
    const deps: Deps = {
      fetch: (...args) => globalThis.fetch(...args),
      keys: googleKeys((...args) => globalThis.fetch(...args)),
      now: Date.now,
    };
    return handle(request, env, deps);
  },
} satisfies ExportedHandler<Env>;
