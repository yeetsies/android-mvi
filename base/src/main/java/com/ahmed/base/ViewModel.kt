package com.ahmed.base

import io.reactivex.Observable

interface ViewModel<I : Intent, S : ViewState> {
    fun processIntents(intents: Observable<I>)

    fun states(): Observable<S>
}
