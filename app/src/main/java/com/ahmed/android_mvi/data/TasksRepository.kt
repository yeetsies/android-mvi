package com.ahmed.android_mvi.data

import io.reactivex.Completable
import io.reactivex.Single

class TasksRepository {
    fun getTasks(forceUpdate: Boolean): Single<List<Task>> {
        return Single.just(listOf())
    }

    fun activateTask(task: Task): Completable = Completable.complete()

    fun completeTask(task: Task): Completable = Completable.complete()

    fun clearCompletedTasks(): Completable = Completable.complete()
}
