// Writes one imported file into OPFS with a sync access handle, for browsers without createWritable (RF-110).
self.onmessage = async (e) => {
  const { folder, name, file } = e.data;
  let access;
  try {
    const root = await navigator.storage.getDirectory();
    const dir = await root.getDirectoryHandle(folder, { create: true });
    const handle = await dir.getFileHandle(name, { create: true });
    access = await handle.createSyncAccessHandle();
    access.truncate(0);
    const reader = file.stream().getReader();
    let at = 0;
    for (;;) {
      const { done, value } = await reader.read();
      if (done) break;
      access.write(value, { at });
      at += value.byteLength;
    }
    access.flush();
    self.postMessage('ok');
  } catch (err) {
    self.postMessage(String(err && err.message ? err.message : err));
  } finally {
    if (access) access.close();
  }
};
