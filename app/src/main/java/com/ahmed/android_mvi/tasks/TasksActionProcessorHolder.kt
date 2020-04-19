package com.ahmed.android_mvi.tasks

import com.ahmed.android_mvi.data.TasksRepository
import com.ahmed.android_mvi.tasks.TasksAction.*
import com.ahmed.android_mvi.tasks.TasksResult.*
import com.ahmed.android_mvi.util.pairWithDelay
import com.ahmed.base.BaseSchedulerProvider
import io.reactivex.Observable
import io.reactivex.ObservableTransformer

class TasksActionProcessorHolder(
    private val tasksRepository: TasksRepository,
    private val schedulerProvider: BaseSchedulerProvider
) {

    private val loadTasksProcessor =
        ObservableTransformer<LoadTasksAction, LoadTasksResult> { actions ->
            actions.flatMap { action ->
                tasksRepository.getTasks(action.forceUpdate)
                    .toObservable()
                    .map { tasks -> LoadTasksResult.Success(tasks, action.filterType) }
                    .cast(LoadTasksResult::class.java)
                    .onErrorReturn(LoadTasksResult::Failure)
                    .subscribeOn(schedulerProvider.io())
                    .observeOn(schedulerProvider.ui())
                    .startWith(LoadTasksResult.InFlight)
            }
        }

    private val activateTaskProcessor =
        ObservableTransformer<ActivateTaskAction, ActivateTaskResult> { actions ->
            actions.flatMap { action ->
                tasksRepository.activateTask(action.task)
                    .andThen(tasksRepository.getTasks(false))
                    .toObservable()
                    .flatMap { tasks ->
                        pairWithDelay(
                            ActivateTaskResult.Success(tasks),
                            ActivateTaskResult.HideUiNotification
                        )
                    }
                    .onErrorReturn(ActivateTaskResult::Failure)
                    .subscribeOn(schedulerProvider.io())
                    .observeOn(schedulerProvider.ui())
                    .startWith(ActivateTaskResult.InFlight)
            }
        }

    private val completeTaskProcessor =
        ObservableTransformer<CompleteTaskAction, CompleteTaskResult> { actions ->
            actions.flatMap { action ->
                tasksRepository.completeTask(action.task)
                    .andThen(tasksRepository.getTasks(false))
                    .toObservable()
                    .flatMap { tasks ->
                        pairWithDelay(
                            CompleteTaskResult.Success(tasks),
                            CompleteTaskResult.HideUiNotification
                        )
                    }
                    .onErrorReturn(CompleteTaskResult::Failure)
                    .subscribeOn(schedulerProvider.io())
                    .observeOn(schedulerProvider.ui())
                    .startWith(CompleteTaskResult.InFlight)
            }
        }

    private val clearCompletedTasksProcessor =
        ObservableTransformer<ClearCompletedTasksAction, ClearCompletedTasksResult> { actions ->
            actions.flatMap {
                tasksRepository.clearCompletedTasks()
                    .andThen(tasksRepository.getTasks(false))
                    .toObservable()
                    .flatMap { tasks ->
                        pairWithDelay(
                            ClearCompletedTasksResult.Success(tasks),
                            ClearCompletedTasksResult.HideUiNotification
                        )
                    }
                    .onErrorReturn(ClearCompletedTasksResult::Failure)
                    .subscribeOn(schedulerProvider.io())
                    .observeOn(schedulerProvider.ui())
                    .startWith(ClearCompletedTasksResult.InFlight)
            }
        }

    internal var actionProcessor =
        ObservableTransformer<TasksAction, TasksResult> { actions ->
            actions.publish { shared ->
                Observable.merge(
                    shared.ofType(LoadTasksAction::class.java).compose(
                        loadTasksProcessor
                    ),
                    shared.ofType(ActivateTaskAction::class.java)
                        .compose(activateTaskProcessor),
                    shared.ofType(CompleteTaskAction::class.java)
                        .compose(completeTaskProcessor),
                    shared.ofType(ClearCompletedTasksAction::class.java)
                        .compose(clearCompletedTasksProcessor)
                ).mergeWith(
                    shared.filter { v ->
                        v !is LoadTasksAction
                                && v !is ActivateTaskAction
                                && v !is CompleteTaskAction
                                && v !is ClearCompletedTasksAction
                    }.flatMap { w ->
                        Observable.error<TasksResult>(
                            IllegalArgumentException("Unknown Action type: $w")
                        )
                    }
                )
            }
        }
}
