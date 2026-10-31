package com.tiagohs.hqr.data.db

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

/** The library in Library/Application Support: kept by the device backup, never shown in Files. */
@OptIn(ExperimentalForeignApi::class)
fun iosDatabase(): HqrDatabase {
    val support = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true).first() as String
    NSFileManager.defaultManager.createDirectoryAtPath(support, withIntermediateDirectories = true, attributes = null, error = null)
    return Room
        .databaseBuilder<HqrDatabase>("$support/${HqrDatabase.NAME}")
        .setDriver(BundledSQLiteDriver())
        .buildHqr()
}

/** In-memory database for the iOS tests: the same bundled SQLite as the app. */
fun inMemoryDatabase(): HqrDatabase =
    Room
        .inMemoryDatabaseBuilder<HqrDatabase>()
        .setDriver(BundledSQLiteDriver())
        .buildHqr()
