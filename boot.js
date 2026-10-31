// SCR-WEB-004 (RF-114, RNF-31): browsers without WebAssembly GC get a clear message instead of a blank page.
(() => {
  const wasmGc = (() => {
    try { return WebAssembly.validate(new Uint8Array([0, 97, 115, 109, 1, 0, 0, 0, 1, 5, 1, 95, 1, 120, 0])); } catch (e) { return false; }
  })();
  if (!wasmGc) {
    document.getElementById('unsupported').style.display = 'block';
    return;
  }
  const s = document.createElement('script');
  s.src = 'hqr.js';
  s.type = 'application/javascript';
  document.body.appendChild(s);
})();
