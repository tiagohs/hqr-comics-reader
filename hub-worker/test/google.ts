// Signs Google-like ID tokens with a local RSA key.
import type { KeySource } from "../src/google";

export const WEB_CLIENT = "web-client.apps.googleusercontent.com";
export const IOS_CLIENT = "ios-client.apps.googleusercontent.com";
export const TIAGO = "tiago@example.com";

function b64url(data: Uint8Array | string): string {
  const bytes = typeof data === "string" ? new TextEncoder().encode(data) : data;
  return Buffer.from(bytes).toString("base64url");
}

export async function fakeGoogle() {
  const pair = (await crypto.subtle.generateKey(
    { name: "RSASSA-PKCS1-v1_5", modulusLength: 2048, publicExponent: new Uint8Array([1, 0, 1]), hash: "SHA-256" },
    true,
    ["sign", "verify"],
  )) as CryptoKeyPair;
  const jwk = { ...(await crypto.subtle.exportKey("jwk", pair.publicKey)), kid: "k1" };
  const keys: KeySource = async () => [jwk];
  const nowSeconds = Math.floor(Date.now() / 1000);
  async function token(claims: Record<string, unknown> = {}, kid = "k1"): Promise<string> {
    const header = b64url(JSON.stringify({ alg: "RS256", kid, typ: "JWT" }));
    const body = b64url(
      JSON.stringify({
        iss: "https://accounts.google.com",
        aud: WEB_CLIENT,
        sub: "1234",
        email: TIAGO,
        email_verified: true,
        exp: nowSeconds + 3600,
        iat: nowSeconds,
        ...claims,
      }),
    );
    const signature = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", pair.privateKey, new TextEncoder().encode(`${header}.${body}`));
    return `${header}.${body}.${b64url(new Uint8Array(signature))}`;
  }
  return { keys, token, nowSeconds };
}
