// The bridge to the online sources, which send no CORS headers (ADR-023, now a route of the personal
// Worker behind the sign-in session, ADR-027). Nothing is stored, cached or logged (RNF-32).

export const DEFAULT_HOSTS: string[] = [
  "api.mangadex.org",
  "uploads.mangadex.org",
  "*.mangadex.network",
  "api.mangadex.network",
  "admin.hq-now.com",
  "www.hq-now.com",
  "*.hq-now.com",
  // Sources added in version 2 (RF-99 to RF-104); MangaFlix sends CORS headers and skips the bridge.
  "archive.org",
  "api.namicomi.com",
  "uploads.namicomi.com",
  "yomumangas.com",
  "api.yomumangas.com",
  "b2.yomumangas.com",
  "weebcentral.com",
  "temp.compsci88.com",
  "*.planeptune.us",
  "xoxocomic.com",
  "*.bp.blogspot.com",
];

export const DEFAULT_USER_AGENT = "HQR (https://github.com/tiagohs/hqr-comics-reader)";

export const MAX_REDIRECTS = 5;

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

/** Splits a comma-separated env value; falls back to the default list when empty. */
export function parseList(value: unknown, fallback: string[]): string[] {
  if (typeof value !== "string" || value.trim() === "") return [...fallback];
  return value
    .split(",")
    .map((item) => item.trim().toLowerCase())
    .filter((item) => item !== "");
}

/** Exact match, or `*.example.com` matching any subdomain (never the apex itself). */
export function hostMatches(host: string, pattern: string): boolean {
  const target = host.toLowerCase();
  const rule = pattern.toLowerCase();
  if (rule.startsWith("*.")) {
    const suffix = rule.slice(1); // ".example.com"
    return target.length > suffix.length && target.endsWith(suffix);
  }
  return target === rule;
}

export function isHostAllowed(host: string, patterns: string[]): boolean {
  return patterns.some((pattern) => hostMatches(host, pattern));
}

/**
 * Validates the `url` query value. Returns `{ url }` or `{ error: "url" | "host" }`.
 * Only https on the default port, without credentials, to an allowed host.
 */
export function parseTarget(raw: string | null, hostPatterns: string[]): { url?: URL; error?: "url" | "host" } {
  if (typeof raw !== "string" || raw === "") return { error: "url" };
  let url: URL;
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
export function buildUpstreamHeaders(incoming: Headers): Headers {
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
export function buildResponseHeaders(upstream: Headers): Headers {
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

/**
 * Fetches the target following redirects by hand, so every hop is checked against the
 * host allowlist. Returns the final Response, or null when a hop leaves the allowlist.
 */
export async function fetchUpstream(
  target: URL,
  method: string,
  headers: Headers,
  body: ArrayBuffer | undefined,
  hostPatterns: string[],
  fetchImpl: typeof fetch,
): Promise<Response | null> {
  let url = target;
  let currentMethod = method;
  let currentBody: ArrayBuffer | undefined = body;
  for (let hop = 0; hop <= MAX_REDIRECTS; hop++) {
    const response = await fetchImpl(url.toString(), {
      method: currentMethod,
      headers,
      body: currentBody,
      redirect: "manual",
    });
    const location = response.headers.get("location");
    if (response.status < 300 || response.status > 399 || !location) return response;
    const next = parseTarget(new URL(location, url).toString(), hostPatterns);
    if (next.error || !next.url) return null;
    if (response.status === 303 || ((response.status === 301 || response.status === 302) && currentMethod === "POST")) {
      currentMethod = "GET";
      currentBody = undefined;
      headers.delete("content-type");
    }
    url = next.url;
  }
  return null;
}

/** GET|HEAD|POST /bridge/fetch?url=…, already authorised by the caller. */
export async function handleBridgeFetch(
  request: Request,
  hostsValue: string | undefined,
  fetchImpl: typeof fetch,
  json: (status: number, payload: unknown) => Response,
): Promise<Response> {
  const hosts = parseList(hostsValue, DEFAULT_HOSTS);
  if (!ALLOWED_METHODS.includes(request.method)) return json(405, { error: "method" });
  const target = parseTarget(new URL(request.url).searchParams.get("url"), hosts);
  if (target.error === "url" || !target.url) return json(target.error === "host" ? 403 : 400, { error: target.error ?? "url" });

  const headers = buildUpstreamHeaders(request.headers);
  // Buffered so a redirect can resend it; source API bodies are small JSON/GraphQL.
  const body = request.method === "POST" ? await request.arrayBuffer() : undefined;
  let upstream: Response | null;
  try {
    upstream = await fetchUpstream(target.url, request.method, headers, body, hosts, fetchImpl);
  } catch {
    return json(502, { error: "upstream" });
  }
  // A redirect to a host outside the list: the source moved, the bridge is not out of date.
  if (upstream === null) return json(403, { error: "redirect" });
  const passBody = request.method !== "HEAD" && upstream.status !== 204 && upstream.status !== 304;
  return new Response(passBody ? upstream.body : null, {
    status: upstream.status,
    headers: buildResponseHeaders(upstream.headers),
  });
}
