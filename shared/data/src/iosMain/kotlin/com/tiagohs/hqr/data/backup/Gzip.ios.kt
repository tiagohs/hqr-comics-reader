package com.tiagohs.hqr.data.backup

import com.tiagohs.hqr.data.zlib.Zlib

/** The backup file (RF-75): gzip with the system zlib. */
actual fun gzip(bytes: ByteArray): ByteArray = Zlib.gzip(bytes)

actual fun gunzip(bytes: ByteArray): ByteArray = Zlib.gunzip(bytes) ?: throw BackupException("Arquivo de backup corrompido")
