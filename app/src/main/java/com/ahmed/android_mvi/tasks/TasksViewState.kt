package com.ahmed.android_mvi.tasks

import com.ahmed.android_mvi.data.Task
import com.ahmed.android_mvi.tasks.TasksFilterType.ALL_TASKS
import com.ahmed.base.BaseViewState

data class TasksViewState(
    val isLoading: Boolean,
    val tasksFilterType: TasksFilterType,
    val tasks: List<Task>,
    val error: Throwable?,
    val uiNotification: UiNotification?
) : BaseViewState {
    enum class UiNotification {
        TASK_COMPLETE,
        TASK_ACTIVATED,
        COMPLETE_TASKS_CLEARED
    }

    companion object {
        fun idle(): TasksViewState {
            return TasksViewState(
                isLoading = false,
                tasksFilterType = ALL_TASKS,
                tasks = emptyList(),
                error = null,
                uiNotification = null
            )
        }
    }
}
