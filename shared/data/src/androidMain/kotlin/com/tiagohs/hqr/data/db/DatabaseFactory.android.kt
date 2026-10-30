package com.tiagohs.hqr.data.db

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

fun androidDatabase(context: Context): HqrDatabase =
    Room
        .databaseBuilder<HqrDatabase>(context, context.getDatabasePath(HqrDatabase.NAME).absolutePath)
        .setDriver(BundledSQLiteDriver())
        .buildHqr()
