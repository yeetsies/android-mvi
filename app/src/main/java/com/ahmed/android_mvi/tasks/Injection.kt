package com.ahmed.android_mvi.tasks

import android.content.Context
import com.ahmed.android_mvi.data.TasksRepository
import com.ahmed.base.BaseSchedulerProvider
import com.ahmed.base.SchedulerProvider

object Injection {
  fun provideTasksRepository(context: Context): TasksRepository {
    return TasksRepository()
  }

  fun provideSchedulerProvider(): BaseSchedulerProvider = SchedulerProvider
}
