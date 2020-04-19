package com.ahmed.android_mvi.util

fun String?.isNullOrEmpty() = this == null || this.isEmpty()
fun String?.isNotNullNorEmpty() = !this.isNullOrEmpty()
