package com.ahmed.android_mvi.tasks

import com.ahmed.android_mvi.data.Task
import com.ahmed.base.BaseIntent

sealed class TasksIntent : BaseIntent {
    object InitialIntent : TasksIntent()
    data class RefreshIntent(val forceUpdate: Boolean) : TasksIntent()
    data class ActivateTaskIntent(val task: Task) : TasksIntent()
    data class CompleteTaskIntent(val task: Task) : TasksIntent()
    object ClearCompletedTasksIntent : TasksIntent()
    data class ChangeFilterIntent(val filterType: TasksFilterType) : TasksIntent()
}
