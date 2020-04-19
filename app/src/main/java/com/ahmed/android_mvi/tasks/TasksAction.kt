package com.ahmed.android_mvi.tasks

import com.ahmed.android_mvi.data.Task
import com.ahmed.base.BaseAction

sealed class TasksAction: BaseAction {
    data class LoadTasksAction(
        val forceUpdate: Boolean,
        val filterType: TasksFilterType?
    ) : TasksAction()

    data class ActivateTaskAction(val task: Task) : TasksAction()

    data class CompleteTaskAction(val task: Task) : TasksAction()

    object ClearCompletedTasksAction : TasksAction()
}
