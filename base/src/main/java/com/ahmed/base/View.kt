package com.ahmed.base

import io.reactivex.Observable

interface View<I : Intent, in S : ViewState> {
    fun intents(): Observable<I>

    fun render(state: S)
}
