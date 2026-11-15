// Checks a Google ID token: RS256 signature against Google's public keys, audience, issuer,
// expiry and a verified email (RNF-34). No Google SDK.

export const GOOGLE_CERTS_URL = "https://www.googleapis.com/oauth2/v3/certs";
export const GOOGLE_ISSUERS = ["accounts.google.com", "https://accounts.google.com"];
const CLOCK_SKEW_SECONDS = 60;

export interface GoogleIdentity {
  email: string;
  subject: string;
}

export type VerifyResult = { identity: GoogleIdentity } | { error: "invalid_token" };

interface Jwk extends JsonWebKey {
  kid?: string;
}

export type KeySource = () => Promise<Jwk[]>;

let cachedKeys: { keys: Jwk[]; until: number } | null = null;

/** Google's keys, kept in memory for the time its Cache-Control allows. */
export function googleKeys(fetchImpl: typeof fetch = fetch, now: () => number = Date.now): KeySource {
  return async () => {
    if (cachedKeys && cachedKeys.until > now()) return cachedKeys.keys;
    const response = await fetchImpl(GOOGLE_CERTS_URL);
    if (!response.ok) throw new Error(`certs ${response.status}`);
    const body = (await response.json()) as { keys: Jwk[] };
    const maxAge = /max-age=(\d+)/.exec(response.headers.get("cache-control") ?? "")?.[1];
    cachedKeys = { keys: body.keys, until: now() + Number(maxAge ?? 3600) * 1000 };
    return body.keys;
  };
}

export function resetKeyCache(): void {
  cachedKeys = null;
}

function base64UrlDecode(part: string): Uint8Array {
  const base64 = part.replace(/-/g, "+").replace(/_/g, "/").padEnd(Math.ceil(part.length / 4) * 4, "=");
  const binary = atob(base64);
  return Uint8Array.from(binary, (char) => char.charCodeAt(0));
}

function decodeJson(part: string): Record<string, unknown> | null {
  try {
    return JSON.parse(new TextDecoder().decode(base64UrlDecode(part))) as Record<string, unknown>;
  } catch {
    return null;
  }
}

export async function verifyGoogleIdToken(
  token: string,
  audiences: string[],
  keys: KeySource,
  nowSeconds: number = Math.floor(Date.now() / 1000),
): Promise<VerifyResult> {
  const invalid = { error: "invalid_token" } as const;
  const parts = typeof token === "string" ? token.split(".") : [];
  if (parts.length !== 3) return invalid;
  const header = decodeJson(parts[0]);
  const claims = decodeJson(parts[1]);
  if (!header || !claims || header.alg !== "RS256" || typeof header.kid !== "string") return invalid;

  const jwk = (await keys()).find((key) => key.kid === header.kid);
  if (!jwk) return invalid;
  const key = await crypto.subtle.importKey(
    "jwk",
    { kty: jwk.kty, n: jwk.n, e: jwk.e, alg: "RS256", ext: true },
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["verify"],
  );
  const signed = new TextEncoder().encode(`${parts[0]}.${parts[1]}`);
  const valid = await crypto.subtle.verify("RSASSA-PKCS1-v1_5", key, base64UrlDecode(parts[2]), signed);
  if (!valid) return invalid;

  const exp = Number(claims.exp);
  const verified = claims.email_verified === true || claims.email_verified === "true";
  if (
    !GOOGLE_ISSUERS.includes(String(claims.iss)) ||
    !audiences.includes(String(claims.aud)) ||
    !Number.isFinite(exp) ||
    exp + CLOCK_SKEW_SECONDS < nowSeconds ||
    !verified ||
    typeof claims.email !== "string" ||
    typeof claims.sub !== "string"
  ) {
    return invalid;
  }
  return { identity: { email: claims.email.toLowerCase(), subject: claims.sub } };
}
