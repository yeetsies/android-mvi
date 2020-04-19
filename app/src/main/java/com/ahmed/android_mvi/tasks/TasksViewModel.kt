package com.ahmed.android_mvi.tasks

import androidx.lifecycle.ViewModel
import com.ahmed.android_mvi.data.Task
import com.ahmed.android_mvi.tasks.TasksAction.*
import com.ahmed.android_mvi.tasks.TasksFilterType.*
import com.ahmed.android_mvi.tasks.TasksResult.*
import com.ahmed.android_mvi.tasks.TasksViewState.UiNotification.*
import com.ahmed.android_mvi.util.notOfType
import com.ahmed.base.BaseViewModel
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.PublishSubject

class TasksViewModel(
    private val actionProcessorHolder: TasksActionProcessorHolder
) : ViewModel(), BaseViewModel<TasksIntent, TasksViewState> {

    private val intentsSubject: PublishSubject<TasksIntent> = PublishSubject.create()
    private val statesObservable: Observable<TasksViewState> = compose()
    private val disposables = CompositeDisposable()

    override fun processIntents(intents: Observable<TasksIntent>) {
        disposables.add(intents.subscribe(intentsSubject::onNext))
    }

    override fun states(): Observable<TasksViewState> = statesObservable

    private val intentFilter: ObservableTransformer<TasksIntent, TasksIntent>
        get() = ObservableTransformer { intents ->
            intents.publish { shared ->
                Observable.merge(
                    shared.ofType(TasksIntent.InitialIntent::class.java).take(1),
                    shared.notOfType(TasksIntent.InitialIntent::class.java)
                )
            }
        }

    private fun compose(): Observable<TasksViewState> {
        return intentsSubject
            .compose(intentFilter)
            .map(this::actionFromIntent)
            .compose(actionProcessorHolder.actionProcessor)
            .scan(TasksViewState.idle(), reducer)
            .distinctUntilChanged()
            .replay(1)
            .autoConnect(0)
    }

    private fun actionFromIntent(intent: TasksIntent): TasksAction {
        return when (intent) {
            is TasksIntent.InitialIntent -> LoadTasksAction(true, ALL_TASKS)
            is TasksIntent.RefreshIntent -> LoadTasksAction(intent.forceUpdate, null)
            is TasksIntent.ActivateTaskIntent -> ActivateTaskAction(intent.task)
            is TasksIntent.CompleteTaskIntent -> CompleteTaskAction(intent.task)
            is TasksIntent.ClearCompletedTasksIntent -> ClearCompletedTasksAction
            is TasksIntent.ChangeFilterIntent -> LoadTasksAction(false, intent.filterType)
        }
    }

    companion object {
        private val reducer = BiFunction { previousState: TasksViewState, result: TasksResult ->
            when (result) {
                is LoadTasksResult -> when (result) {
                    is LoadTasksResult.Success -> {
                        val filterType = result.filterType ?: previousState.tasksFilterType
                        val tasks = filteredTasks(result.tasks, filterType)
                        previousState.copy(
                            isLoading = false,
                            tasks = tasks,
                            tasksFilterType = filterType
                        )
                    }
                    is LoadTasksResult.Failure -> previousState.copy(
                        isLoading = false,
                        error = result.error
                    )
                    is LoadTasksResult.InFlight -> previousState.copy(isLoading = true)
                }
                is CompleteTaskResult -> when (result) {
                    is CompleteTaskResult.Success ->
                        previousState.copy(
                            uiNotification = TASK_COMPLETE,
                            tasks = filteredTasks(result.tasks, previousState.tasksFilterType)
                        )
                    is CompleteTaskResult.Failure -> previousState.copy(error = result.error)
                    is CompleteTaskResult.InFlight -> previousState
                    is CompleteTaskResult.HideUiNotification ->
                        if (previousState.uiNotification == TASK_COMPLETE) {
                            previousState.copy(uiNotification = null)
                        } else {
                            previousState
                        }
                }
                is ActivateTaskResult -> when (result) {
                    is ActivateTaskResult.Success ->
                        previousState.copy(
                            uiNotification = TASK_ACTIVATED,
                            tasks = filteredTasks(result.tasks, previousState.tasksFilterType)
                        )
                    is ActivateTaskResult.Failure -> previousState.copy(error = result.error)
                    is ActivateTaskResult.InFlight -> previousState
                    is ActivateTaskResult.HideUiNotification ->
                        if (previousState.uiNotification == TASK_ACTIVATED) {
                            previousState.copy(uiNotification = null)
                        } else {
                            previousState
                        }
                }
                is ClearCompletedTasksResult -> when (result) {
                    is ClearCompletedTasksResult.Success ->
                        previousState.copy(
                            uiNotification = COMPLETE_TASKS_CLEARED,
                            tasks = filteredTasks(result.tasks, previousState.tasksFilterType)
                        )
                    is ClearCompletedTasksResult.Failure -> previousState.copy(error = result.error)
                    is ClearCompletedTasksResult.InFlight -> previousState
                    is ClearCompletedTasksResult.HideUiNotification ->
                        if (previousState.uiNotification == COMPLETE_TASKS_CLEARED) {
                            previousState.copy(uiNotification = null)
                        } else {
                            previousState
                        }
                }
            }
        }

        private fun filteredTasks(
            tasks: List<Task>,
            filterType: TasksFilterType
        ): List<Task> {
            return when (filterType) {
                ALL_TASKS -> tasks
                ACTIVE_TASKS -> tasks.filter(Task::active)
                COMPLETED_TASKS -> tasks.filter(Task::completed)
            }
        }
    }
}
