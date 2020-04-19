package com.ahmed.android_mvi.util

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ahmed.android_mvi.tasks.Injection
import com.ahmed.android_mvi.tasks.TasksActionProcessorHolder
import com.ahmed.android_mvi.tasks.TasksViewModel

class ToDoViewModelFactory private constructor(
    private val applicationContext: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == TasksViewModel::class.java) {
            return TasksViewModel(
                TasksActionProcessorHolder(
                    Injection.provideTasksRepository(applicationContext),
                    Injection.provideSchedulerProvider())
            ) as T
        }

        throw IllegalArgumentException("unknown model class $modelClass")
    }

    companion object : SingletonHolderSingleArg<ToDoViewModelFactory, Context>(::ToDoViewModelFactory)
}
