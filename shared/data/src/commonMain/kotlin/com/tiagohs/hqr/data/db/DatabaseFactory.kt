package com.tiagohs.hqr.data.db

import androidx.room3.RoomDatabase
import kotlinx.coroutines.CoroutineDispatcher

/** Dispatcher for disk and database work: IO on Android and the JVM, Default in the browser. */
expect val ioDispatcher: CoroutineDispatcher

/** Applies the settings every platform shares and builds the database. */
fun RoomDatabase.Builder<HqrDatabase>.buildHqr(): HqrDatabase = setQueryCoroutineContext(ioDispatcher).build()
