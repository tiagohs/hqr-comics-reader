// HQR personal bridge: a Cloudflare Worker that forwards the web app's requests to the
// online sources (MangaDex, HQ Now), which send no CORS headers. Each user publishes it on
// their own account (ADR-023, RF-108, RF-109, RNF-32). Nothing is stored, cached or logged.

export const BRIDGE_VERSION = 1;

export const DEFAULT_HOSTS = [
  "api.mangadex.org",
  "uploads.mangadex.org",
  "*.mangadex.network",
  "api.mangadex.network",
  "admin.hq-now.com",
  "www.hq-now.com",
  "*.hq-now.com",
];

export const DEFAULT_ORIGINS = ["https://tiagohs.github.io", "http://localhost:8080"];

export const DEFAULT_USER_AGENT = "HQR (https://github.com/tiagohs/hqr-comics-reader)";

export const MIN_TOKEN_LENGTH = 32;
export const MAX_REDIRECTS = 5;

export const TOKEN_HEADER = "X-HQR-Token";
export const UA_HEADER = "X-HQR-UA";
export const REFERER_HEADER = "X-HQR-Referer";

// Request headers copied from the app to the source. Everything else is dropped.
export const FORWARDED_REQUEST_HEADERS = ["accept", "accept-language", "content-type", "range"];

// Response headers copied from the source back to the app (plus every x-ratelimit-*).
export const FORWARDED_RESPONSE_HEADERS = [
  "content-type",
  "content-length",
  "content-range",
  "accept-ranges",
  "cache-control",
  "retry-after",
];

export const ALLOWED_METHODS = ["GET", "HEAD", "POST"];

export const CORS_ALLOW_HEADERS = [TOKEN_HEADER, UA_HEADER, REFERER_HEADER, "Content-Type", "Range", "Accept"];

export const CORS_EXPOSE_HEADERS = [
  "Content-Length",
  "Content-Type",
  "Content-Range",
  "Retry-After",
  "X-RateLimit-Limit",
  "X-RateLimit-Remaining",
  "X-RateLimit-Retry-After",
];

/** Splits a comma-separated env value; falls back to the default list when empty. */
export function parseList(value, fallback) {
  if (typeof value !== "string" || value.trim() === "") return [...fallback];
  return value
    .split(",")
    .map((item) => item.trim().toLowerCase())
    .filter((item) => item !== "");
}

/** Exact match, or `*.example.com` matching any subdomain (never the apex itself). */
export function hostMatches(host, pattern) {
  const target = host.toLowerCase();
  const rule = pattern.toLowerCase();
  if (rule.startsWith("*.")) {
    const suffix = rule.slice(1); // ".example.com"
    return target.length > suffix.length && target.endsWith(suffix);
  }
  return target === rule;
}

export function isHostAllowed(host, patterns) {
  return patterns.some((pattern) => hostMatches(host, pattern));
}

/** Compares in time that depends only on the expected length, not on where the strings differ. */
export function constantTimeEquals(given, expected) {
  if (typeof given !== "string" || typeof expected !== "string") return false;
  const encoder = new TextEncoder();
  const a = encoder.encode(given);
  const b = encoder.encode(expected);
  let diff = a.length ^ b.length;
  for (let i = 0; i < b.length; i++) {
    diff |= (a[i % (a.length || 1)] ?? 0) ^ b[i];
  }
  return diff === 0;
}

/** A missing or short TOKEN secret locks the bridge: every request is refused. */
export function isTokenValid(given, secret) {
  if (typeof secret !== "string" || secret.length < MIN_TOKEN_LENGTH) return false;
  return constantTimeEquals(given ?? "", secret);
}

/**
 * Validates the `url` query value. Returns `{ url }` or `{ error: "url" | "host" }`.
 * Only https on the default port, without credentials, to an allowed host.
 */
export function parseTarget(raw, hostPatterns) {
  if (typeof raw !== "string" || raw === "") return { error: "url" };
  let url;
  try {
    url = new URL(raw);
  } catch {
    return { error: "url" };
  }
  if (url.username !== "" || url.password !== "") return { error: "url" };
  if (url.protocol !== "https:" || url.port !== "") return { error: "host" };
  if (!isHostAllowed(url.hostname, hostPatterns)) return { error: "host" };
  return { url };
}

/** Builds the upstream headers from scratch: allowlist, HQR User-Agent and optional Referer. */
export function buildUpstreamHeaders(incoming) {
  const headers = new Headers();
  for (const name of FORWARDED_REQUEST_HEADERS) {
    const value = incoming.get(name);
    if (value !== null) headers.set(name, value);
  }
  headers.set("User-Agent", incoming.get(UA_HEADER) || DEFAULT_USER_AGENT);
  const referer = incoming.get(REFERER_HEADER);
  if (referer) headers.set("Referer", referer);
  return headers;
}

/** Copies only the allowlisted response headers and the rate-limit family. */
export function buildResponseHeaders(upstream) {
  const headers = new Headers();
  const encoding = (upstream.get("content-encoding") || "identity").toLowerCase();
  for (const name of FORWARDED_RESPONSE_HEADERS) {
    // The runtime decodes compressed bodies, so the upstream length would be wrong.
    if (name === "content-length" && encoding !== "identity") continue;
    const value = upstream.get(name);
    if (value !== null) headers.set(name, value);
  }
  for (const [name, value] of upstream) {
    if (name.toLowerCase().startsWith("x-ratelimit-")) headers.set(name, value);
  }
  return headers;
}

/** CORS headers for an allowed Origin; an empty object otherwise. */
export function corsHeaders(origin, allowedOrigins) {
  if (!origin || !allowedOrigins.includes(origin.toLowerCase())) return {};
  return {
    "Access-Control-Allow-Origin": origin,
    "Access-Control-Expose-Headers": CORS_EXPOSE_HEADERS.join(", "),
  };
}

export function preflightHeaders(origin, allowedOrigins) {
  const base = corsHeaders(origin, allowedOrigins);
  if (!base["Access-Control-Allow-Origin"]) return {};
  return {
    ...base,
    "Access-Control-Allow-Methods": [...ALLOWED_METHODS, "OPTIONS"].join(", "),
    "Access-Control-Allow-Headers": CORS_ALLOW_HEADERS.join(", "),
    "Access-Control-Max-Age": "86400",
  };
}

export function jsonResponse(status, payload, extraHeaders = {}) {
  return new Response(JSON.stringify(payload), {
    status,
    headers: { "Content-Type": "application/json", "Cache-Control": "no-store", ...extraHeaders },
  });
}

function withHeaders(response, extra) {
  for (const [name, value] of Object.entries(extra)) response.headers.set(name, value);
  return response;
}

/**
 * Fetches the target following redirects by hand, so every hop is checked against the
 * host allowlist. Returns the final Response, or null when a hop leaves the allowlist.
 */
export async function fetchUpstream(target, method, headers, body, hostPatterns, fetchImpl) {
  let url = target;
  let currentMethod = method;
  let currentBody = body;
  for (let hop = 0; hop <= MAX_REDIRECTS; hop++) {
    const response = await fetchImpl(url.toString(), {
      method: currentMethod,
      headers,
      body: currentBody,
      redirect: "manual",
      cache: "no-store",
    });
    const location = response.headers.get("location");
    if (response.status < 300 || response.status > 399 || !location) return response;
    const next = parseTarget(new URL(location, url).toString(), hostPatterns);
    if (next.error) return null;
    if (response.status === 303 || ((response.status === 301 || response.status === 302) && currentMethod === "POST")) {
      currentMethod = "GET";
      currentBody = undefined;
      headers.delete("content-type");
    }
    url = next.url;
  }
  return null;
}

export async function handleRequest(request, env = {}, fetchImpl = (...args) => globalThis.fetch(...args)) {
  const origins = parseList(env.ORIGINS, DEFAULT_ORIGINS);
  const hosts = parseList(env.HOSTS, DEFAULT_HOSTS);
  const origin = request.headers.get("Origin");
  const cors = { ...corsHeaders(origin, origins), Vary: "Origin" };
  const path = new URL(request.url).pathname;

  // Preflight carries no custom headers, so it is answered without the token.
  if (request.method === "OPTIONS") {
    return new Response(null, { status: 204, headers: { ...preflightHeaders(origin, origins), Vary: "Origin" } });
  }

  if (!isTokenValid(request.headers.get(TOKEN_HEADER), env.TOKEN)) {
    return jsonResponse(403, { error: "token" }, cors);
  }

  if (path === "/health") {
    return jsonResponse(200, { ok: true, version: BRIDGE_VERSION }, cors);
  }

  if (path !== "/fetch") return jsonResponse(404, { error: "path" }, cors);
  if (!ALLOWED_METHODS.includes(request.method)) return jsonResponse(405, { error: "method" }, cors);

  const target = parseTarget(new URL(request.url).searchParams.get("url"), hosts);
  if (target.error === "url") return jsonResponse(400, { error: "url" }, cors);
  if (target.error === "host") return jsonResponse(403, { error: "host" }, cors);

  const headers = buildUpstreamHeaders(request.headers);
  // Buffered so a redirect can resend it; source API bodies are small JSON/GraphQL.
  const body = request.method === "POST" ? await request.arrayBuffer() : undefined;

  let upstream;
  try {
    upstream = await fetchUpstream(target.url, request.method, headers, body, hosts, fetchImpl);
  } catch {
    return jsonResponse(502, { error: "upstream" }, cors);
  }
  if (upstream === null) return jsonResponse(403, { error: "host" }, cors);

  const responseHeaders = buildResponseHeaders(upstream.headers);
  const passBody = request.method !== "HEAD" && upstream.status !== 204 && upstream.status !== 304;
  return withHeaders(
    new Response(passBody ? upstream.body : null, { status: upstream.status, headers: responseHeaders }),
    cors,
  );
}

export default {
  fetch(request, env) {
    return handleRequest(request, env);
  },
};
