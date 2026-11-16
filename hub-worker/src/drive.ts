// "Meu Drive" (RF-130): reads the user's own folder through a Google service account and serves it to the app.
// The folder `hqr` (DRIVE_ROOT_ID) holds `mangas/` and `hqs/`; each work is a folder with a poster, an info JSON and
// one subfolder per chapter ("01", "02"…) full of page images, or one PDF per chapter ("01.pdf"). No Google SDK: a signed JWT for the token, REST for the rest.

const DRIVE_API = "https://www.googleapis.com/drive/v3/files";
const TOKEN_URL = "https://oauth2.googleapis.com/token";
const SCOPE = "https://www.googleapis.com/auth/drive.readonly";
const FOLDER = "application/vnd.google-apps.folder";
const ID_PATTERN = /^[A-Za-z0-9_-]{10,100}$/;
const POSTER_PATTERN = /^poster\.(png|jpe?g|webp)$/i;
const PDF_PATTERN = /\.pdf$/i;
const IMAGE_PATTERN = /\.(png|jpe?g|webp|gif|avif)$/i;
const SHELVES = ["mangas", "hqs"] as const;
const PARENTS_PER_QUERY = 40;
const MAX_LIST_PAGES = 20;
const TOKEN_MARGIN_SECONDS = 120;
const IMAGE_CACHE = "private, max-age=86400";

export interface DriveEnv {
  /** The service account's JSON key (secret). */
  DRIVE_SERVICE_ACCOUNT?: string;
  /** The id of the `hqr` folder, shared with the service account's email. */
  DRIVE_ROOT_ID?: string;
}

interface DriveFile {
  id: string;
  name: string;
  mimeType: string;
  modifiedTime?: string;
  parents?: string[];
}

type Json = (status: number, payload: unknown) => Response;

let cachedToken: { token: string; until: number } | null = null;

export function resetDriveToken(): void {
  cachedToken = null;
}

function base64Url(bytes: ArrayBuffer | Uint8Array | string): string {
  const data = typeof bytes === "string" ? new TextEncoder().encode(bytes) : new Uint8Array(bytes);
  let binary = "";
  for (const byte of data) binary += String.fromCharCode(byte);
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

async function signJwt(account: { client_email: string; private_key: string }, nowSeconds: number): Promise<string> {
  const pem = account.private_key.replace(/-----[A-Z ]+-----/g, "").replace(/\s+/g, "");
  const der = Uint8Array.from(atob(pem), (char) => char.charCodeAt(0));
  const key = await crypto.subtle.importKey("pkcs8", der, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["sign"]);
  const header = base64Url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const claims = base64Url(
    JSON.stringify({ iss: account.client_email, scope: SCOPE, aud: TOKEN_URL, iat: nowSeconds, exp: nowSeconds + 3600 }),
  );
  const signature = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(`${header}.${claims}`));
  return `${header}.${claims}.${base64Url(signature)}`;
}

async function accessToken(env: DriveEnv, fetchImpl: typeof fetch, nowMs: number): Promise<string | null> {
  const nowSeconds = Math.floor(nowMs / 1000);
  if (cachedToken && cachedToken.until > nowSeconds) return cachedToken.token;
  let account: { client_email?: string; private_key?: string };
  try {
    account = JSON.parse(env.DRIVE_SERVICE_ACCOUNT ?? "");
  } catch {
    return null;
  }
  if (!account.client_email || !account.private_key) return null;
  const assertion = await signJwt({ client_email: account.client_email, private_key: account.private_key }, nowSeconds);
  const response = await fetchImpl(TOKEN_URL, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion }),
  });
  if (!response.ok) return null;
  const body = (await response.json()) as { access_token?: string; expires_in?: number };
  if (!body.access_token) return null;
  cachedToken = { token: body.access_token, until: nowSeconds + (body.expires_in ?? 3600) - TOKEN_MARGIN_SECONDS };
  return body.access_token;
}

/** Numbers inside names compare as numbers: "page2" before "page10", "01" before "2". */
export function naturalCompare(a: string, b: string): number {
  const parts = (text: string) => text.toLowerCase().match(/\d+|\D+/g) ?? [];
  const left = parts(a);
  const right = parts(b);
  for (let i = 0; i < Math.min(left.length, right.length); i++) {
    const x = left[i];
    const y = right[i];
    if (x === y) continue;
    const bothNumbers = /^\d/.test(x) && /^\d/.test(y);
    if (bothNumbers) {
      const diff = Number(x) - Number(y);
      if (diff !== 0) return diff;
      // 01 and 1 are the same number: the shorter one first keeps the order stable.
      if (x.length !== y.length) return x.length - y.length;
    } else {
      return x < y ? -1 : 1;
    }
  }
  return left.length - right.length;
}

class DriveError extends Error {
  constructor(readonly status: number) {
    super(`drive ${status}`);
  }
}

async function driveGet(url: string, token: string, fetchImpl: typeof fetch): Promise<Response> {
  const response = await fetchImpl(url, { headers: { Authorization: `Bearer ${token}` } });
  if (!response.ok && response.status !== 404) throw new DriveError(response.status);
  return response;
}

async function listFiles(query: string, token: string, fetchImpl: typeof fetch): Promise<DriveFile[]> {
  const found: DriveFile[] = [];
  let pageToken: string | undefined;
  for (let page = 0; page < MAX_LIST_PAGES; page++) {
    const params = new URLSearchParams({
      q: `${query} and trashed=false`,
      fields: "nextPageToken,files(id,name,mimeType,modifiedTime,parents)",
      pageSize: "1000",
    });
    if (pageToken) params.set("pageToken", pageToken);
    const response = await driveGet(`${DRIVE_API}?${params}`, token, fetchImpl);
    if (response.status === 404) return found;
    const body = (await response.json()) as { files?: DriveFile[]; nextPageToken?: string };
    found.push(...(body.files ?? []));
    pageToken = body.nextPageToken;
    if (!pageToken) break;
  }
  return found;
}

const children = (parentId: string) => `'${parentId}' in parents`;
const isFolder = (file: DriveFile) => file.mimeType === FOLDER;
const isPdf = (file: DriveFile) => !isFolder(file) && (file.mimeType === "application/pdf" || PDF_PATTERN.test(file.name));

interface WorkSummary {
  id: string;
  shelf: (typeof SHELVES)[number];
  name: string;
  posterId: string | null;
  modifiedTime: string | null;
}

async function listWorks(rootId: string, token: string, fetchImpl: typeof fetch): Promise<WorkSummary[]> {
  const top = (await listFiles(children(rootId), token, fetchImpl)).filter(isFolder);
  const works: WorkSummary[] = [];
  for (const shelf of SHELVES) {
    const folder = top.find((item) => item.name.toLowerCase() === shelf);
    if (!folder) continue;
    const folders = (await listFiles(children(folder.id), token, fetchImpl)).filter(isFolder);
    for (const work of folders) {
      works.push({ id: work.id, shelf, name: work.name, posterId: null, modifiedTime: work.modifiedTime ?? null });
    }
  }
  const byId = new Map(works.map((work) => [work.id, work]));
  const ids = [...byId.keys()];
  for (let start = 0; start < ids.length; start += PARENTS_PER_QUERY) {
    const group = ids.slice(start, start + PARENTS_PER_QUERY);
    const query = `(${group.map(children).join(" or ")}) and name contains 'poster'`;
    for (const file of await listFiles(query, token, fetchImpl)) {
      const work = file.parents?.map((parent) => byId.get(parent)).find((item) => item !== undefined);
      if (work && POSTER_PATTERN.test(file.name) && work.posterId === null) work.posterId = file.id;
    }
  }
  return works.sort((a, b) => naturalCompare(a.name, b.name));
}

async function workDetails(workId: string, token: string, fetchImpl: typeof fetch) {
  const files = await listFiles(children(workId), token, fetchImpl);
  const poster = files.find((file) => POSTER_PATTERN.test(file.name));
  const infoFile = files.filter((file) => file.name.toLowerCase().endsWith(".json")).sort((a, b) => naturalCompare(a.name, b.name))[0];
  let info: unknown = null;
  if (infoFile) {
    const response = await driveGet(`${DRIVE_API}/${infoFile.id}?alt=media`, token, fetchImpl);
    if (response.ok) {
      try {
        info = JSON.parse(await response.text());
      } catch {
        info = null;
      }
    }
  }
  // A chapter is a folder of pages or one PDF named after it ("01.pdf").
  const chapters = files
    .filter((file) => isFolder(file) || isPdf(file))
    .map((file) => ({
      id: file.id,
      name: isFolder(file) ? file.name : file.name.replace(PDF_PATTERN, ""),
      modifiedTime: file.modifiedTime ?? null,
      kind: isFolder(file) ? "folder" : "pdf",
    }))
    .sort((a, b) => naturalCompare(a.name, b.name));
  return { info, posterId: poster?.id ?? null, chapters };
}

async function chapterPages(chapterId: string, token: string, fetchImpl: typeof fetch) {
  const files = await listFiles(children(chapterId), token, fetchImpl);
  const pages = files
    .filter((file) => !isFolder(file) && (file.mimeType.startsWith("image/") || IMAGE_PATTERN.test(file.name)))
    .sort((a, b) => naturalCompare(a.name, b.name))
    .map((file) => ({ id: file.id, name: file.name }));
  // A folder with no images but a PDF is read as that PDF.
  const pdf = pages.length === 0 ? (files.filter(isPdf).sort((a, b) => naturalCompare(a.name, b.name))[0]?.id ?? null) : null;
  return { pages, pdf };
}

async function fileBytes(fileId: string, token: string, fetchImpl: typeof fetch): Promise<Response | null> {
  const response = await driveGet(`${DRIVE_API}/${fileId}?alt=media`, token, fetchImpl);
  const type = response.headers.get("content-type") ?? "";
  // Only pictures and PDFs leave through here, whatever else the folder holds.
  if (!response.ok || !(type.startsWith("image/") || type === "application/pdf")) return null;
  return new Response(response.body, { status: 200, headers: { "Content-Type": type, "Cache-Control": IMAGE_CACHE } });
}

/** GET /api/v1/drive/{works|works/:id|chapters/:id|files/:id}, already authorised by the caller. */
export async function handleDrive(
  request: Request,
  env: DriveEnv,
  fetchImpl: typeof fetch,
  now: () => number,
  json: Json,
): Promise<Response> {
  if (request.method !== "GET") return json(405, { error: "method" });
  const rootId = env.DRIVE_ROOT_ID ?? "";
  if (!ID_PATTERN.test(rootId) || !env.DRIVE_SERVICE_ACCOUNT) return json(503, { error: "not_configured" });
  const [, , , , kind, id] = new URL(request.url).pathname.split("/");
  if ((kind !== "works" || id !== undefined) && !ID_PATTERN.test(id ?? "")) return json(400, { error: "id" });

  try {
    const token = await accessToken(env, fetchImpl, now());
    if (!token) return json(503, { error: "not_configured" });
    switch (kind) {
      case "works":
        return id === undefined ? json(200, { works: await listWorks(rootId, token, fetchImpl) }) : json(200, await workDetails(id, token, fetchImpl));
      case "chapters":
        return json(200, await chapterPages(id, token, fetchImpl));
      case "files": {
        const image = await fileBytes(id, token, fetchImpl);
        return image ?? json(404, { error: "file" });
      }
      default:
        return json(404, { error: "path" });
    }
  } catch (error) {
    // Google refused the service account (401/403): the sharing or the key needs attention.
    if (error instanceof DriveError && (error.status === 401 || error.status === 403)) return json(502, { error: "drive_access" });
    return json(502, { error: "drive" });
  }
}
