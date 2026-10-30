package com.tiagohs.hqr.data.backup

// The browser's backup (CompressionStream) arrives with the web milestone (FEAT-052); the Android app has it now.
actual fun gzip(bytes: ByteArray): ByteArray = throw BackupException("Backup ainda não roda no navegador")

actual fun gunzip(bytes: ByteArray): ByteArray = throw BackupException("Backup ainda não roda no navegador")
