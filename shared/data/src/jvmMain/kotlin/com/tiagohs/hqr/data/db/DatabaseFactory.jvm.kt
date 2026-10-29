package com.tiagohs.hqr.data.db

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

/** In-memory database for tests: the same SQLite as the app (bundled driver). */
fun inMemoryDatabase(): HqrDatabase =
    Room
        .inMemoryDatabaseBuilder<HqrDatabase>()
        .setDriver(BundledSQLiteDriver())
        .buildHqr()
