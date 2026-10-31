package com.tiagohs.hqr.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** An ongoing subscription; SwiftUI cancels it when the screen goes away. */
class Subscription internal constructor(
    private val job: Job,
) {
    fun cancel() = job.cancel()

    val active: Boolean get() = job.isActive
}

/**
 * Bridges a StateFlow to Swift with the standard Kotlin/Native export: Swift gets the current state right away and
 * every change after that, on the main thread.
 */
class Observer<T : Any>(
    private val flow: StateFlow<T>,
    private val scope: CoroutineScope,
) {
    /** Swift does not see parameters with default values: this is its constructor. */
    constructor(flow: StateFlow<T>) : this(flow, MainScope())

    val current: T get() = flow.value

    fun observe(onChange: (T) -> Unit): Subscription = Subscription(scope.launch(Dispatchers.Main) { flow.collect { onChange(it) } })
}

/** Holds the ViewModels of an iPhone or iPad screen while it exists; [close] cancels what they were doing. */
class ScreenHolder {
    private val screens = ViewModelStore()

    fun store(
        key: String,
        viewModel: ViewModel,
    ) = screens.put(key, viewModel)

    fun close() = screens.clear()
}

/**
 * One iPhone or iPad screen: keeps its ViewModel alive while the screen exists and forwards [flow] to Swift. [close] cancels
 * what the ViewModel was doing (Swift calls it when the screen goes away).
 */
class Screen<T : Any>(
    viewModel: ViewModel,
    flow: StateFlow<T>,
) {
    private val store = ViewModelStore().also { it.put(KEY, viewModel) }
    private val observer = Observer(flow)

    val current: T get() = observer.current

    fun observe(onChange: (T) -> Unit): Subscription = observer.observe(onChange)

    fun close() = store.clear()

    private companion object {
        const val KEY = "screen"
    }
}
