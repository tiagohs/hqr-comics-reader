// Imported files in the browser storage (RF-110, FEAT-052): one OPFS folder, flat; the Kotlin app reads them by ranges.
(() => {
  const FOLDER = 'hqr-files';
  const ACCEPTED = ['cbz', 'zip', 'pdf'];
  // CBR and CB7 don't open in the browser yet: refused with the reason (RF-110).
  const REFUSED = ['cbr', 'rar', 'cb7', '7z'];
  const listeners = [];
  const ext = (name) => (name.split('.').pop() || '').toLowerCase();

  async function folder() {
    const root = await navigator.storage.getDirectory();
    return root.getDirectoryHandle(FOLDER, { create: true });
  }

  // Safari before 26 has no createWritable: a small worker writes with a sync access handle instead.
  function writeInWorker(name, file) {
    return new Promise((resolve, reject) => {
      const worker = new Worker(new URL('opfs-writer.js', document.baseURI));
      worker.onmessage = (e) => { worker.terminate(); e.data === 'ok' ? resolve() : reject(new Error(e.data)); };
      worker.onerror = (e) => { worker.terminate(); reject(new Error(e.message)); };
      worker.postMessage({ folder: FOLDER, name, file });
    });
  }

  async function write(dir, file) {
    const handle = await dir.getFileHandle(file.name, { create: true });
    if (handle.createWritable) {
      const out = await handle.createWritable();
      await file.stream().pipeTo(out);
    } else {
      await writeInWorker(file.name, file);
    }
  }

  async function save(files) {
    const result = { imported: [], refused: [], ignored: [], failed: [] };
    let dir;
    try { dir = await folder(); } catch (e) { result.failed = files.map((f) => f.name); return JSON.stringify(result); }
    for (const file of files) {
      const kind = ext(file.name);
      if (REFUSED.includes(kind)) { result.refused.push(file.name); continue; }
      if (!ACCEPTED.includes(kind)) { result.ignored.push(file.name); continue; }
      try {
        await write(dir, file);
        result.imported.push(file.name);
      } catch (e) {
        // No half file stays behind (a full disk, a blocked storage).
        result.failed.push(file.name);
        try { await dir.removeEntry(file.name); } catch (_) { /* nothing was written */ }
      }
    }
    if (result.imported.length) listeners.forEach((cb) => cb());
    return JSON.stringify(result);
  }

  function pick() {
    return new Promise((resolve) => {
      const input = document.createElement('input');
      input.type = 'file';
      input.multiple = true;
      input.accept = '.cbz,.zip,.pdf,.cbr,.rar,.cb7,.7z';
      input.onchange = () => resolve(save(Array.from(input.files || [])));
      input.oncancel = () => resolve(JSON.stringify({ imported: [], refused: [], ignored: [], failed: [] }));
      input.click();
    });
  }

  async function list() {
    const dir = await folder();
    const out = [];
    for await (const [name, handle] of dir.entries()) {
      if (handle.kind !== 'file') continue;
      const file = await handle.getFile();
      out.push({ name, size: file.size, modified: file.lastModified });
    }
    return JSON.stringify(out);
  }

  async function read(name, offset, length) {
    const dir = await folder();
    const file = await (await dir.getFileHandle(name)).getFile();
    return new Int8Array(await file.slice(offset, offset + length).arrayBuffer());
  }

  async function remove(name) {
    const dir = await folder();
    await dir.removeEntry(name);
    listeners.forEach((cb) => cb());
  }

  async function usage() {
    const e = await navigator.storage.estimate();
    return JSON.stringify({ used: e.usage || 0, quota: e.quota || 0 });
  }

  // PDF pages by pdf.js (ADR-011), the open document kept for the next pages.
  let openPdf = null;
  function pdfDocument(name) {
    // The promise is kept, so pages asked together share one load and never destroy each other's document.
    if (openPdf && openPdf.name === name) return openPdf.promise;
    const previous = openPdf;
    const promise = (async () => {
      const lib = await import(new URL('pdf.min.mjs', document.baseURI).href);
      lib.GlobalWorkerOptions.workerSrc = new URL('pdf.worker.min.mjs', document.baseURI).href;
      const dir = await folder();
      const file = await (await dir.getFileHandle(name)).getFile();
      return lib.getDocument({ data: new Uint8Array(await file.arrayBuffer()), isEvalSupported: false }).promise;
    })();
    openPdf = { name, promise };
    if (previous) previous.promise.then((doc) => doc.destroy(), () => {});
    promise.catch(() => { if (openPdf && openPdf.promise === promise) openPdf = null; });
    return promise;
  }

  async function pdfPageCount(name) {
    return (await pdfDocument(name)).numPages;
  }

  async function pdfRender(name, index) {
    const page = await (await pdfDocument(name)).getPage(index + 1);
    const viewport = page.getViewport({ scale: 2 });
    const canvas = new OffscreenCanvas(Math.ceil(viewport.width), Math.ceil(viewport.height));
    await page.render({ canvasContext: canvas.getContext('2d'), viewport }).promise;
    page.cleanup();
    const blob = await canvas.convertToBlob({ type: 'image/jpeg', quality: 0.9 });
    return new Int8Array(await blob.arrayBuffer());
  }

  async function persist() {
    return navigator.storage.persist ? navigator.storage.persist() : false;
  }

  // Dropping files anywhere on the window imports them.
  window.addEventListener('dragover', (e) => { if (e.dataTransfer && Array.from(e.dataTransfer.types).includes('Files')) e.preventDefault(); });
  window.addEventListener('drop', (e) => {
    if (!e.dataTransfer || !e.dataTransfer.files.length) return;
    e.preventDefault();
    const files = Array.from(e.dataTransfer.files);
    save(files)
      .catch(() => JSON.stringify({ imported: [], refused: [], ignored: [], failed: files.map((f) => f.name) }))
      .then((json) => window.dispatchEvent(new CustomEvent('hqr-dropped', { detail: json })));
  });

  window.hqrFiles = {
    available: !!(navigator.storage && navigator.storage.getDirectory),
    pick, list, read, remove, usage, persist, pdfPageCount, pdfRender,
    onChange: (cb) => listeners.push(cb),
    onDrop: (cb) => window.addEventListener('hqr-dropped', (e) => cb(e.detail)),
  };
})();
