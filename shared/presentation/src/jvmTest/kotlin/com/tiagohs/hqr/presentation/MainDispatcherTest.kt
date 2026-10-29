package com.tiagohs.hqr.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

/**
 * ViewModels run on a real single-thread Main, like the app. An unconfined test Main would resume Room's callers
 * inline on its own threads and can deadlock on the connection. Every ViewModel is cleared after the test.
 */
@OptIn(ExperimentalCoroutinesApi::class, kotlinx.coroutines.DelicateCoroutinesApi::class)
abstract class MainDispatcherTest {
    private lateinit var main: ExecutorCoroutineDispatcher
    private val store = ViewModelStore()
    private var count = 0

    @BeforeTest
    fun setMain() {
        main = newSingleThreadContext("test-main")
        Dispatchers.setMain(main)
    }

    @AfterTest
    fun resetMain() {
        store.clear()
        Thread.sleep(50)
        Dispatchers.resetMain()
        main.close()
    }

    /** Keeps [vm] to be cleared (scope cancelled, onCleared called) at the end of the test. */
    fun <T : ViewModel> track(vm: T): T = vm.also { store.put("vm${count++}", it) }
}
