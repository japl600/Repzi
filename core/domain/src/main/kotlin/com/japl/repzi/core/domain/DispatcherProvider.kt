package com.japl.repzi.core.domain

import kotlinx.coroutines.CoroutineDispatcher

/** Permite inyectar dispatchers de test en lugar de usar Dispatchers.* directamente. */
interface DispatcherProvider {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}
