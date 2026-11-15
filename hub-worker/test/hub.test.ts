import { beforeEach, describe, expect, it } from "vitest";
import { verifyGoogleIdToken } from "../src/google";
import { handle, type Deps, type Env } from "../src/index";
import { hashToken } from "../src/session";
import { testDb, type TestDb } from "./d1";
import { IOS_CLIENT, TIAGO, WEB_CLIENT, fakeGoogle } from "./google";

const BASE = "https://hqr.example.workers.dev";
const DAY = 86_400_000;

let store: TestDb;
let google: Awaited<ReturnType<typeof fakeGoogle>>;
let now: number;
let upstream: { url: string; init?: RequestInit }[];
let env: Env;
let deps: Deps;

beforeEach(async () => {
  store = testDb();
  google = await fakeGoogle();
  now = Date.now();
  upstream = [];
  env = { DB: store.db, ALLOWED_EMAILS: ` ${TIAGO.toUpperCase()} `, GOOGLE_CLIENT_IDS: `${WEB_CLIENT},${IOS_CLIENT}` };
  deps = {
    keys: google.keys,
    now: () => now,
    fetch: (async (input: RequestInfo | URL, init?: RequestInit) => {
      upstream.push({ url: String(input), init });
      return new Response('{"data":[]}', { status: 200, headers: { "content-type": "application/json", "set-cookie": "x=1" } });
    }) as typeof fetch,
  };
});

function call(path: string, init: RequestInit & { token?: string } = {}) {
  const headers = new Headers(init.headers);
  if (init.token) headers.set("Authorization", `Bearer ${init.token}`);
  return handle(new Request(BASE + path, { ...init, headers }), env, deps);
}

async function signIn(claims: Record<string, unknown> = {}) {
  const response = await call("/api/v1/auth/google", {
    method: "POST",
    body: JSON.stringify({ idToken: await google.token(claims), device: "Pixel 8" }),
  });
  return { response, body: (await response.json()) as { token: string; email: string; expiresAt: number; error?: string } };
}

const change = (key: string, changedAt: number, payload = `{"k":"${key}"}`, deleted = false) => ({
  kind: "manga",
  key,
  payload,
  changedAt,
  deleted,
});

async function syncCall(token: string, cursor: number, changes: unknown[] = []) {
  const response = await call("/api/v1/sync", { method: "POST", token, body: JSON.stringify({ cursor, changes }) });
  return { status: response.status, body: (await response.json()) as { cursor: number; changes: { key: string; seq: number; payload: string; deleted: boolean }[]; more: boolean } };
}

describe("sign-in", () => {
  it("RF119_allowed_google_account_gets_a_90_day_session", async () => {
    const { response, body } = await signIn();
    expect(response.status).toBe(200);
    expect(body.email).toBe(TIAGO);
    expect(body.expiresAt).toBe(now + 90 * DAY);
    expect((await call("/api/v1/me", { token: body.token })).status).toBe(200);
  });

  it("RF119_ios_client_audience_is_accepted", async () => {
    expect((await signIn({ aud: IOS_CLIENT })).response.status).toBe(200);
  });

  it("RF120_email_outside_the_list_gets_not_allowed_and_no_session", async () => {
    const { response, body } = await signIn({ email: "someone@example.com" });
    expect(response.status).toBe(403);
    expect(body.error).toBe("not_allowed");
    expect(store.raw.prepare("SELECT COUNT(*) AS n FROM session").get()).toEqual({ n: 0 });
    expect(store.raw.prepare("SELECT COUNT(*) AS n FROM account").get()).toEqual({ n: 0 });
  });

  it("RF120_without_secrets_nobody_signs_in", async () => {
    env.ALLOWED_EMAILS = "";
    expect((await signIn()).response.status).toBe(503);
  });

  it("RNF34_session_is_stored_only_as_its_hash", async () => {
    const { body } = await signIn();
    const row = store.raw.prepare("SELECT tokenHash FROM session").get() as { tokenHash: string };
    expect(row.tokenHash).toBe(await hashToken(body.token));
    expect(row.tokenHash).not.toContain(body.token);
    expect(Buffer.from(body.token, "base64url").length).toBe(32);
  });

  it("RNF34_rejects_bad_audience_issuer_expiry_unverified_email_and_signature", async () => {
    const audiences = [WEB_CLIENT];
    const check = async (token: string) => verifyGoogleIdToken(token, audiences, google.keys, google.nowSeconds);
    expect(await check(await google.token({ aud: "other" }))).toEqual({ error: "invalid_token" });
    expect(await check(await google.token({ iss: "https://evil.example" }))).toEqual({ error: "invalid_token" });
    expect(await check(await google.token({ exp: google.nowSeconds - 3600 }))).toEqual({ error: "invalid_token" });
    expect(await check(await google.token({ email_verified: false }))).toEqual({ error: "invalid_token" });
    expect(await check(await google.token({}, "unknown-kid"))).toEqual({ error: "invalid_token" });
    const good = await google.token();
    const [h, , s] = good.split(".");
    const forged = Buffer.from(JSON.stringify({ iss: "accounts.google.com", aud: WEB_CLIENT, email: TIAGO, email_verified: true, sub: "1", exp: google.nowSeconds + 60 })).toString("base64url");
    expect(await check(`${h}.${forged}.${s}`)).toEqual({ error: "invalid_token" });
    expect(await check("not-a-jwt")).toEqual({ error: "invalid_token" });
  });

  it("RF119_session_renews_with_use_and_expires_unused", async () => {
    const { body } = await signIn();
    now += 30 * DAY;
    expect((await call("/api/v1/me", { token: body.token })).status).toBe(200);
    now += 89 * DAY;
    expect((await call("/api/v1/me", { token: body.token })).status).toBe(200);
    now += 91 * DAY;
    expect((await call("/api/v1/me", { token: body.token })).status).toBe(401);
  });

  it("RF119_sign_out_ends_the_session", async () => {
    const { body } = await signIn();
    expect((await call("/api/v1/auth/signout", { method: "POST", token: body.token })).status).toBe(200);
    expect((await call("/api/v1/me", { token: body.token })).status).toBe(401);
  });

  it("RF119_api_without_session_is_refused", async () => {
    expect((await call("/api/v1/sync", { method: "POST", body: "{}" })).status).toBe(401);
    expect((await call("/api/v1/me", { token: "x".repeat(43) })).status).toBe(401);
  });

  it("RF119_config_gives_the_web_client_id", async () => {
    expect(await (await call("/api/v1/config")).json()).toEqual({ googleClientId: WEB_CLIENT });
  });
});

describe("sync", () => {
  it("RF121_changes_from_one_device_reach_the_other", async () => {
    const phone = (await signIn()).body.token;
    const web = (await signIn()).body.token;
    const sent = await syncCall(phone, 0, [change("mangadex/1", 100), change("mangadex/2", 100)]);
    expect(sent.body.cursor).toBe(2);
    const got = await syncCall(web, 0);
    expect(got.body.changes.map((c) => c.key)).toEqual(["mangadex/1", "mangadex/2"]);
    expect(got.body.cursor).toBe(2);
    expect((await syncCall(web, 2)).body.changes).toEqual([]);
  });

  it("RF123_an_older_change_never_replaces_a_newer_one", async () => {
    const token = (await signIn()).body.token;
    await syncCall(token, 0, [change("a", 200, '{"v":"new"}')]);
    await syncCall(token, 1, [change("a", 100, '{"v":"old"}')]);
    const all = await syncCall(token, 0);
    expect(all.body.changes).toHaveLength(1);
    expect(all.body.changes[0].payload).toBe('{"v":"new"}');
  });

  it("RF123_removal_travels_as_a_tombstone", async () => {
    const token = (await signIn()).body.token;
    await syncCall(token, 0, [change("a", 100)]);
    await syncCall(token, 1, [change("a", 200, "{}", true)]);
    const got = await syncCall(token, 1);
    expect(got.body.changes).toMatchObject([{ key: "a", deleted: true }]);
  });

  it("RF121_large_backlog_comes_in_pages", async () => {
    const token = (await signIn()).body.token;
    await syncCall(token, 0, Array.from({ length: 700 }, (_, i) => change(`k${i}`, 100)));
    const first = await syncCall(token, 0);
    expect(first.body.changes).toHaveLength(500);
    expect(first.body.more).toBe(true);
    const second = await syncCall(token, first.body.cursor);
    expect(second.body.changes).toHaveLength(200);
    expect(second.body.more).toBe(false);
  });

  it("RF121_accounts_never_see_each_other", async () => {
    env.ALLOWED_EMAILS = `${TIAGO},other@example.com`;
    const tiago = (await signIn()).body.token;
    const other = (await signIn({ email: "other@example.com" })).body.token;
    await syncCall(tiago, 0, [change("a", 100)]);
    expect((await syncCall(other, 0)).body.changes).toEqual([]);
  });

  it("RF121_rejects_unknown_kinds_and_bad_bodies", async () => {
    const token = (await signIn()).body.token;
    expect((await syncCall(token, 0, [{ ...change("a", 1), kind: "download" }])).status).toBe(400);
    expect((await call("/api/v1/sync", { method: "POST", token, body: "nope" })).status).toBe(400);
  });

  it("RNF35_a_cycle_without_changes_reads_at_most_two_rows", async () => {
    const token = (await signIn()).body.token;
    await syncCall(token, 0, [change("a", 100)]);
    const before = store.reads();
    await syncCall(token, 1);
    expect(store.reads() - before).toBeLessThanOrEqual(2);
  });
});

describe("bridge", () => {
  it("RF108_bridge_needs_the_session", async () => {
    const response = await call(`/bridge/fetch?url=${encodeURIComponent("https://api.mangadex.org/manga")}`);
    expect(response.status).toBe(401);
    expect(upstream).toEqual([]);
  });

  it("RF108_RF109_bridge_forwards_allowed_hosts_and_drops_cookies", async () => {
    const token = (await signIn()).body.token;
    const response = await call(`/bridge/fetch?url=${encodeURIComponent("https://api.mangadex.org/manga")}`, {
      token,
      headers: { "X-HQR-UA": "HQR test", Cookie: "secret=1" },
    });
    expect(response.status).toBe(200);
    expect(response.headers.get("set-cookie")).toBeNull();
    const sent = new Headers(upstream[0].init?.headers);
    expect(sent.get("authorization")).toBeNull();
    expect(sent.get("cookie")).toBeNull();
    expect(sent.get("user-agent")).toBe("HQR test");
  });

  it("RNF32_bridge_refuses_hosts_outside_the_list", async () => {
    const token = (await signIn()).body.token;
    const response = await call(`/bridge/fetch?url=${encodeURIComponent("https://evil.example/")}`, { token });
    expect(response.status).toBe(403);
    expect(upstream).toEqual([]);
  });
});

describe("web app", () => {
  it("RF107_other_paths_are_served_from_the_assets", async () => {
    env.ASSETS = { fetch: async () => new Response("<html>HQR</html>") } as unknown as Fetcher;
    expect(await (await call("/biblioteca")).text()).toContain("HQR");
  });
});
