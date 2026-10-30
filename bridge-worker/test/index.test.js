import { test, beforeEach, afterEach } from "node:test";
import assert from "node:assert/strict";

import worker, {
  DEFAULT_USER_AGENT,
  constantTimeEquals,
  handleRequest,
  hostMatches,
  isTokenValid,
  parseTarget,
  DEFAULT_HOSTS,
} from "../src/index.js";

const TOKEN = "0123456789abcdef0123456789abcdef0123456789";
const ENV = { TOKEN };
const BRIDGE = "https://hqr-bridge.example.workers.dev";
const APP_ORIGIN = "https://tiagohs.github.io";

let calls;
let nextResponse;
const realFetch = globalThis.fetch;

beforeEach(() => {
  calls = [];
  nextResponse = () => new Response("{}", { status: 200, headers: { "Content-Type": "application/json" } });
  globalThis.fetch = async (url, init) => {
    calls.push({ url, init });
    return nextResponse(url, init);
  };
});

afterEach(() => {
  globalThis.fetch = realFetch;
});

function fetchUrl(target) {
  return `${BRIDGE}/fetch?url=${encodeURIComponent(target)}`;
}

function request(url, { method = "GET", headers = {}, body } = {}) {
  return new Request(url, { method, headers, body });
}

const authed = (extra = {}) => ({ "X-HQR-Token": TOKEN, ...extra });

// Health

test("RF108_healthWithValidTokenReturnsOkAndVersion", async () => {
  const response = await handleRequest(request(`${BRIDGE}/health`, { headers: authed() }), ENV);
  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), { ok: true, version: 2 });
});

test("RF108_healthWithWrongTokenIs403", async () => {
  const response = await handleRequest(request(`${BRIDGE}/health`, { headers: { "X-HQR-Token": "x".repeat(42) } }), ENV);
  assert.equal(response.status, 403);
  assert.deepEqual(await response.json(), { error: "token" });
});

test("RF108_defaultExportServesHealth", async () => {
  const response = await worker.fetch(request(`${BRIDGE}/health`, { headers: authed() }), ENV);
  assert.equal(response.status, 200);
});

// Token

test("RF109_requestWithoutTokenIs403", async () => {
  const response = await handleRequest(request(fetchUrl("https://api.mangadex.org/manga")), ENV);
  assert.equal(response.status, 403);
  assert.deepEqual(await response.json(), { error: "token" });
  assert.equal(calls.length, 0);
});

test("RNF32_unsetTokenSecretRefusesEveryRequest", async () => {
  const response = await handleRequest(request(`${BRIDGE}/health`, { headers: { "X-HQR-Token": "" } }), {});
  assert.equal(response.status, 403);
  assert.deepEqual(await response.json(), { error: "token" });
});

test("RNF32_tokenSecretShorterThan32CharsRefusesEveryRequest", async () => {
  const short = "short-token";
  const response = await handleRequest(request(`${BRIDGE}/health`, { headers: { "X-HQR-Token": short } }), { TOKEN: short });
  assert.equal(response.status, 403);
});

test("RNF32_constantTimeEqualsComparesWholeValue", () => {
  assert.equal(constantTimeEquals(TOKEN, TOKEN), true);
  assert.equal(constantTimeEquals(TOKEN.slice(0, -1), TOKEN), false);
  assert.equal(constantTimeEquals(TOKEN + "x", TOKEN), false);
  assert.equal(constantTimeEquals("", TOKEN), false);
  assert.equal(isTokenValid(undefined, TOKEN), false);
});

// Target validation

test("RF109_hostOutsideAllowlistIs403", async () => {
  const response = await handleRequest(request(fetchUrl("https://evil.example.com/x"), { headers: authed() }), ENV);
  assert.equal(response.status, 403);
  assert.deepEqual(await response.json(), { error: "host" });
  assert.equal(calls.length, 0);
});

test("RF109_httpTargetIs403", async () => {
  const response = await handleRequest(request(fetchUrl("http://api.mangadex.org/manga"), { headers: authed() }), ENV);
  assert.equal(response.status, 403);
  assert.deepEqual(await response.json(), { error: "host" });
});

test("RF109_nonDefaultPortIs403", () => {
  assert.deepEqual(parseTarget("https://api.mangadex.org:8443/manga", DEFAULT_HOSTS), { error: "host" });
});

test("RF109_missingOrInvalidUrlIs400", async () => {
  for (const url of [`${BRIDGE}/fetch`, `${BRIDGE}/fetch?url=not%20a%20url`, fetchUrl("https://u:p@api.mangadex.org/")]) {
    const response = await handleRequest(request(url, { headers: authed() }), ENV);
    assert.equal(response.status, 400, url);
    assert.deepEqual(await response.json(), { error: "url" });
  }
});

test("RF109_wildcardMatchesSubdomainsButNotApexOrLookalikes", () => {
  assert.equal(hostMatches("abc123.def.mangadex.network", "*.mangadex.network"), true);
  assert.equal(hostMatches("mangadex.network", "*.mangadex.network"), false);
  assert.equal(hostMatches("evilmangadex.network", "*.mangadex.network"), false);
  assert.equal(hostMatches("mangadex.network.evil.com", "*.mangadex.network"), false);
});

test("RF109_wildcardHostIsForwarded", async () => {
  const target = "https://xyz.abc.mangadex.network/data/hash/1.png";
  const response = await handleRequest(request(fetchUrl(target), { headers: authed() }), ENV);
  assert.equal(response.status, 200);
  assert.equal(calls[0].url, target);
});

test("RF109_hostsEnvReplacesDefaults", async () => {
  const env = { TOKEN, HOSTS: "*.example.org" };
  const ok = await handleRequest(request(fetchUrl("https://a.example.org/"), { headers: authed() }), env);
  const refused = await handleRequest(request(fetchUrl("https://api.mangadex.org/"), { headers: authed() }), env);
  assert.equal(ok.status, 200);
  assert.equal(refused.status, 403);
});

test("RF109_redirectToHostOutsideAllowlistIs403", async () => {
  nextResponse = () => new Response(null, { status: 302, headers: { Location: "https://evil.example.com/" } });
  const response = await handleRequest(request(fetchUrl("https://api.mangadex.org/x"), { headers: authed() }), ENV);
  assert.equal(response.status, 403);
  assert.deepEqual(await response.json(), { error: "redirect" });
  assert.equal(calls.length, 1);
});

test("RF109_redirectInsideAllowlistIsFollowed", async () => {
  nextResponse = (url) =>
    url.includes("/old")
      ? new Response(null, { status: 301, headers: { Location: "/new" } })
      : new Response("moved", { status: 200 });
  const response = await handleRequest(request(fetchUrl("https://api.mangadex.org/old"), { headers: authed() }), ENV);
  assert.equal(response.status, 200);
  assert.equal(await response.text(), "moved");
  assert.equal(calls[1].url, "https://api.mangadex.org/new");
});

// Upstream headers

test("RF109_viaForwardedForAndTokenAreNeverForwarded", async () => {
  const headers = authed({
    Via: "1.1 proxy",
    "X-Forwarded-For": "203.0.113.9",
    "CF-Connecting-IP": "203.0.113.9",
    Authorization: "Bearer secret",
    Cookie: "a=b",
    Accept: "application/json",
    "Accept-Language": "pt-BR",
    Range: "bytes=0-99",
  });
  await handleRequest(request(fetchUrl("https://api.mangadex.org/manga"), { headers }), ENV);
  const sent = calls[0].init.headers;
  for (const name of ["via", "x-forwarded-for", "cf-connecting-ip", "authorization", "cookie", "x-hqr-token", "origin"]) {
    assert.equal(sent.get(name), null, name);
  }
  assert.equal(sent.get("accept"), "application/json");
  assert.equal(sent.get("accept-language"), "pt-BR");
  assert.equal(sent.get("range"), "bytes=0-99");
  assert.equal(calls[0].init.redirect, "manual");
  assert.equal(calls[0].init.cache, "no-store");
});

test("RF109_defaultUserAgentIsHqr", async () => {
  await handleRequest(request(fetchUrl("https://api.mangadex.org/manga"), { headers: authed() }), ENV);
  assert.equal(calls[0].init.headers.get("user-agent"), DEFAULT_USER_AGENT);
  assert.equal(calls[0].init.headers.get("referer"), null);
});

test("RF109_userAgentAndRefererComeFromHqrHeaders", async () => {
  const headers = authed({ "X-HQR-UA": "HQR/1.0 web", "X-HQR-Referer": "https://www.hq-now.com/" });
  await handleRequest(request(fetchUrl("https://admin.hq-now.com/graphql"), { headers }), ENV);
  const sent = calls[0].init.headers;
  assert.equal(sent.get("user-agent"), "HQR/1.0 web");
  assert.equal(sent.get("referer"), "https://www.hq-now.com/");
  assert.equal(sent.get("x-hqr-ua"), null);
  assert.equal(sent.get("x-hqr-referer"), null);
});

// CORS

test("RF108_corsEchoesAllowedOriginOnly", async () => {
  const allowed = await handleRequest(request(`${BRIDGE}/health`, { headers: authed({ Origin: APP_ORIGIN }) }), ENV);
  assert.equal(allowed.headers.get("access-control-allow-origin"), APP_ORIGIN);
  assert.match(allowed.headers.get("access-control-expose-headers"), /X-RateLimit-Remaining/);

  const other = await handleRequest(request(`${BRIDGE}/health`, { headers: authed({ Origin: "https://evil.example" }) }), ENV);
  assert.equal(other.headers.get("access-control-allow-origin"), null);

  const custom = await handleRequest(
    request(`${BRIDGE}/health`, { headers: authed({ Origin: "https://hqr.example.net" }) }),
    { TOKEN, ORIGINS: "https://hqr.example.net" },
  );
  assert.equal(custom.headers.get("access-control-allow-origin"), "https://hqr.example.net");
});

test("RF108_corsHeadersAlsoOnErrors", async () => {
  const response = await handleRequest(request(`${BRIDGE}/health`, { headers: { Origin: APP_ORIGIN } }), ENV);
  assert.equal(response.status, 403);
  assert.equal(response.headers.get("access-control-allow-origin"), APP_ORIGIN);
});

test("RF108_optionsPreflightAnsweredWithoutToken", async () => {
  const response = await handleRequest(
    request(fetchUrl("https://api.mangadex.org/manga"), {
      method: "OPTIONS",
      headers: { Origin: "http://localhost:8080", "Access-Control-Request-Method": "GET" },
    }),
    ENV,
  );
  assert.equal(response.status, 204);
  assert.equal(response.headers.get("access-control-allow-origin"), "http://localhost:8080");
  assert.match(response.headers.get("access-control-allow-headers"), /X-HQR-Token/);
  assert.match(response.headers.get("access-control-allow-headers"), /X-HQR-Referer/);
  assert.equal(response.headers.get("access-control-max-age"), "86400");
  assert.equal(calls.length, 0);
});

test("RF108_optionsPreflightFromUnknownOriginHasNoCors", async () => {
  const response = await handleRequest(
    request(`${BRIDGE}/health`, { method: "OPTIONS", headers: { Origin: "https://evil.example" } }),
    ENV,
  );
  assert.equal(response.headers.get("access-control-allow-origin"), null);
});

// Passthrough

test("RF109_statusBodyAndHeadersArePassedThrough", async () => {
  nextResponse = () =>
    new Response("rate limited", {
      status: 429,
      headers: {
        "Content-Type": "text/plain",
        "Content-Length": "12",
        "Cache-Control": "max-age=60",
        "Retry-After": "30",
        "X-RateLimit-Remaining": "0",
        "X-RateLimit-Retry-After": "1700000000",
        "Set-Cookie": "tracking=1",
        Server: "nginx",
      },
    });
  const response = await handleRequest(
    request(fetchUrl("https://api.mangadex.org/manga"), { headers: authed({ Origin: APP_ORIGIN }) }),
    ENV,
  );
  assert.equal(response.status, 429);
  assert.equal(await response.text(), "rate limited");
  assert.equal(response.headers.get("content-type"), "text/plain");
  assert.equal(response.headers.get("content-length"), "12");
  assert.equal(response.headers.get("cache-control"), "max-age=60");
  assert.equal(response.headers.get("retry-after"), "30");
  assert.equal(response.headers.get("x-ratelimit-remaining"), "0");
  assert.equal(response.headers.get("x-ratelimit-retry-after"), "1700000000");
  assert.equal(response.headers.get("set-cookie"), null);
  assert.equal(response.headers.get("server"), null);
  assert.equal(response.headers.get("access-control-allow-origin"), APP_ORIGIN);
});

test("RF109_imageBytesArePassedThrough", async () => {
  const bytes = new Uint8Array([0x89, 0x50, 0x4e, 0x47, 0, 255]);
  nextResponse = () => new Response(bytes, { status: 200, headers: { "Content-Type": "image/png" } });
  const response = await handleRequest(
    request(fetchUrl("https://uploads.mangadex.org/covers/a/b.jpg"), { headers: authed() }),
    ENV,
  );
  assert.deepEqual(new Uint8Array(await response.arrayBuffer()), bytes);
  assert.equal(response.headers.get("content-type"), "image/png");
});

test("RF109_postBodyIsForwarded", async () => {
  const query = JSON.stringify({ query: "{ getHqsByName(name: \"x\") { id } }" });
  await handleRequest(
    request(fetchUrl("https://admin.hq-now.com/graphql"), {
      method: "POST",
      headers: authed({ "Content-Type": "application/json" }),
      body: query,
    }),
    ENV,
  );
  assert.equal(calls[0].init.method, "POST");
  assert.equal(new TextDecoder().decode(calls[0].init.body), query);
  assert.equal(calls[0].init.headers.get("content-type"), "application/json");
});

test("RF109_headReturnsNoBody", async () => {
  nextResponse = () => new Response("ignored", { status: 200, headers: { "Content-Type": "image/jpeg" } });
  const response = await handleRequest(
    request(fetchUrl("https://uploads.mangadex.org/x.jpg"), { method: "HEAD", headers: authed() }),
    ENV,
  );
  assert.equal(calls[0].init.method, "HEAD");
  assert.equal(response.status, 200);
  assert.equal(response.body, null);
});

test("RF109_contentLengthDroppedForCompressedUpstream", async () => {
  nextResponse = () =>
    new Response("{}", { status: 200, headers: { "Content-Encoding": "gzip", "Content-Length": "5" } });
  const response = await handleRequest(request(fetchUrl("https://api.mangadex.org/manga"), { headers: authed() }), ENV);
  assert.equal(response.headers.get("content-length"), null);
});

test("RF109_otherMethodsAndPathsAreRefused", async () => {
  const put = await handleRequest(request(fetchUrl("https://api.mangadex.org/x"), { method: "PUT", headers: authed(), body: "x" }), ENV);
  assert.equal(put.status, 405);
  const unknown = await handleRequest(request(`${BRIDGE}/other`, { headers: authed() }), ENV);
  assert.equal(unknown.status, 404);
  assert.equal(calls.length, 0);
});

test("RF109_upstreamNetworkErrorIs502", async () => {
  nextResponse = () => {
    throw new TypeError("network");
  };
  const response = await handleRequest(request(fetchUrl("https://api.mangadex.org/x"), { headers: authed() }), ENV);
  assert.equal(response.status, 502);
  assert.deepEqual(await response.json(), { error: "upstream" });
});
