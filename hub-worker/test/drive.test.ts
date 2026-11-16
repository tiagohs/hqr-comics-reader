import { beforeEach, describe, expect, it } from "vitest";
import { handleDrive, naturalCompare, resetDriveToken, type DriveEnv } from "../src/drive";
import { handle, json, type Deps, type Env } from "../src/index";
import { testDb } from "./d1";
import { TIAGO, WEB_CLIENT, fakeGoogle } from "./google";

const BASE = "https://hqr.example.workers.dev";
const ROOT = "rootFolderId0001";
const MANGAS = "mangasFolder0001";
const WORK = "workMonicaJovem1";
const CHAPTER_1 = "chapterFolder0001";
const CHAPTER_2 = "chapterFolder0002";
const FOLDER = "application/vnd.google-apps.folder";

let account: string;
let requests: string[];
let tree: Record<string, { id: string; name: string; mimeType: string; parents: string[]; modifiedTime?: string }[]>;

async function serviceAccount(): Promise<string> {
  const pair = await crypto.subtle.generateKey(
    { name: "RSASSA-PKCS1-v1_5", modulusLength: 2048, publicExponent: new Uint8Array([1, 0, 1]), hash: "SHA-256" },
    true,
    ["sign", "verify"],
  );
  const der = new Uint8Array(await crypto.subtle.exportKey("pkcs8", pair.privateKey));
  let binary = "";
  for (const byte of der) binary += String.fromCharCode(byte);
  const pem = `-----BEGIN PRIVATE KEY-----\n${btoa(binary)}\n-----END PRIVATE KEY-----\n`;
  return JSON.stringify({ client_email: "reader@hqr.iam.gserviceaccount.com", private_key: pem });
}

const file = (id: string, name: string, parent: string, mimeType = "image/png") => ({ id, name, mimeType, parents: [parent] });

function fakeDrive(): typeof fetch {
  return (async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = new URL(String(input));
    requests.push(String(input));
    if (url.host === "oauth2.googleapis.com") return Response.json({ access_token: "drive-token", expires_in: 3600 });
    expect(new Headers(init?.headers).get("Authorization")).toBe("Bearer drive-token");
    const media = /\/files\/([^/]+)$/.exec(url.pathname)?.[1];
    if (media && url.searchParams.get("alt") === "media") {
      if (media === "infoJsonFile0001") return new Response('{"title":"Turma da Mônica Jovem"}', { headers: { "content-type": "application/json" } });
      if (media === "pdfChapter0010") return new Response("%PDF-1.4", { headers: { "content-type": "application/pdf" } });
      if (media === "notAnImage00001") return new Response("secret", { headers: { "content-type": "text/plain" } });
      return new Response(new Uint8Array([1, 2, 3]), { headers: { "content-type": "image/png" } });
    }
    const q = url.searchParams.get("q") ?? "";
    const parents = [...q.matchAll(/'([^']+)' in parents/g)].map((m) => m[1]);
    const files = parents.flatMap((parent) => tree[parent] ?? []).filter((f) => !q.includes("name contains 'poster'") || f.name.startsWith("poster"));
    return Response.json({ files });
  }) as typeof fetch;
}

beforeEach(async () => {
  resetDriveToken();
  requests = [];
  account = await serviceAccount();
  tree = {
    [ROOT]: [
      { id: MANGAS, name: "mangas", mimeType: FOLDER, parents: [ROOT] },
      { id: "hqsFolder000001", name: "hqs", mimeType: FOLDER, parents: [ROOT] },
      file("strayFile000001", "notes.txt", ROOT, "text/plain"),
    ],
    [MANGAS]: [{ id: WORK, name: "Turma da Mônica Jovem", mimeType: FOLDER, parents: [MANGAS] }],
    hqsFolder000001: [],
    [WORK]: [
      file("posterFile00001", "poster.jpg", WORK),
      file("infoJsonFile0001", "info.json", WORK, "application/json"),
      { id: CHAPTER_2, name: "02", mimeType: FOLDER, parents: [WORK] },
      { id: CHAPTER_1, name: "01", mimeType: FOLDER, parents: [WORK] },
      file("pdfChapter0010", "10.pdf", WORK, "application/pdf"),
    ],
    [CHAPTER_1]: [file("page000000010", "pagina-10.png", CHAPTER_1), file("page000000002", "pagina-2.png", CHAPTER_1), file("page000000001", "Pagina-1.jpg", CHAPTER_1)],
    [CHAPTER_2]: [],
    pdfOnlyFolder01: [file("onlyPdfFile0001", "capitulo.pdf", "pdfOnlyFolder01", "application/pdf")],
  };
});

const env = (): DriveEnv => ({ DRIVE_SERVICE_ACCOUNT: account, DRIVE_ROOT_ID: ROOT });
const get = (path: string, e: DriveEnv = env()) => handleDrive(new Request(BASE + path), e, fakeDrive(), () => 1_000_000, json);

describe("Meu Drive", () => {
  it("RF130_pages_come_in_numeric_order_whatever_the_names", () => {
    const names = ["pagina-10.png", "pagina-2.png", "Pagina-1.jpg", "pagina-3.png"];
    expect(names.sort(naturalCompare)).toEqual(["Pagina-1.jpg", "pagina-2.png", "pagina-3.png", "pagina-10.png"]);
  });

  it("RF130_works_list_the_shelf_the_title_and_the_poster", async () => {
    const response = await get("/api/v1/drive/works");
    expect(response.status).toBe(200);
    const body = (await response.json()) as { works: { id: string; shelf: string; name: string; posterId: string | null }[] };
    expect(body.works).toEqual([
      { id: WORK, shelf: "mangas", name: "Turma da Mônica Jovem", posterId: "posterFile00001", modifiedTime: null },
    ]);
  });

  it("RF130_work_details_bring_the_info_and_the_chapters_in_order", async () => {
    const body = (await (await get(`/api/v1/drive/works/${WORK}`)).json()) as { info: { title: string }; chapters: { name: string; kind: string }[]; posterId: string };
    expect(body.info.title).toBe("Turma da Mônica Jovem");
    expect(body.chapters.map((c) => c.name)).toEqual(["01", "02", "10"]);
    expect(body.chapters.map((c) => c.kind)).toEqual(["folder", "folder", "pdf"]);
    expect(body.posterId).toBe("posterFile00001");
  });

  it("RF130_chapter_pages_are_sorted_naturally", async () => {
    const body = (await (await get(`/api/v1/drive/chapters/${CHAPTER_1}`)).json()) as { pages: { name: string }[]; pdf: null };
    expect(body.pages.map((p) => p.name)).toEqual(["Pagina-1.jpg", "pagina-2.png", "pagina-10.png"]);
  });

  it("RF131_a_chapter_can_be_one_pdf_and_a_folder_with_only_a_pdf_reads_as_it", async () => {
    const folder = (await (await get("/api/v1/drive/chapters/pdfOnlyFolder01")).json()) as { pages: unknown[]; pdf: string };
    expect(folder).toEqual({ pages: [], pdf: "onlyPdfFile0001" });
    const pdf = await get("/api/v1/drive/files/pdfChapter0010");
    expect(pdf.headers.get("content-type")).toBe("application/pdf");
  });

  it("RF130_only_images_leave_through_files", async () => {
    const image = await get("/api/v1/drive/files/page000000001");
    expect(image.status).toBe(200);
    expect(image.headers.get("content-type")).toBe("image/png");
    expect((await get("/api/v1/drive/files/notAnImage00001")).status).toBe(404);
  });

  it("RF130_the_token_is_asked_once_for_many_calls", async () => {
    await get("/api/v1/drive/works");
    await get(`/api/v1/drive/chapters/${CHAPTER_1}`);
    expect(requests.filter((url) => url.includes("oauth2.googleapis.com")).length).toBe(1);
  });

  it("RF130_without_the_secrets_the_drive_is_not_configured", async () => {
    expect((await get("/api/v1/drive/works", {})).status).toBe(503);
  });

  it("RF130_an_id_that_is_not_an_id_is_refused_before_google", async () => {
    expect((await get("/api/v1/drive/files/..%2Fx")).status).toBe(400);
    expect(requests).toEqual([]);
  });

  it("RF130_the_drive_needs_the_session", async () => {
    const store = testDb();
    const google = await fakeGoogle();
    const hubEnv: Env = { DB: store.db, ALLOWED_EMAILS: TIAGO, GOOGLE_CLIENT_IDS: WEB_CLIENT, ...env() };
    const deps: Deps = { keys: google.keys, now: Date.now, fetch: fakeDrive() };
    const response = await handle(new Request(`${BASE}/api/v1/drive/works`), hubEnv, deps);
    expect(response.status).toBe(401);
  });
});
