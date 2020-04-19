package com.ahmed.android_mvi.tasks

import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.ahmed.android_mvi.R
import com.ahmed.android_mvi.tasks.TasksFilterType.*
import com.ahmed.android_mvi.tasks.TasksIntent.ActivateTaskIntent
import com.ahmed.android_mvi.tasks.TasksIntent.CompleteTaskIntent
import com.ahmed.android_mvi.tasks.TasksViewState.UiNotification.*
import com.ahmed.android_mvi.util.ToDoViewModelFactory
import com.ahmed.base.BaseView
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rxbinding2.support.v4.widget.RxSwipeRefreshLayout
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import kotlin.LazyThreadSafetyMode.NONE

class TasksFragment: Fragment(), BaseView<TasksIntent, TasksViewState> {
    private lateinit var listAdapter: TasksAdapter
    private lateinit var noTasksView: View
    private lateinit var noTaskIcon: ImageView
    private lateinit var noTaskMainView: TextView
    private lateinit var noTaskAddView: TextView
    private lateinit var tasksView: LinearLayout
    private lateinit var filteringLabelView: TextView
    private lateinit var swipeRefreshLayout: ScrollChildSwipeRefreshLayout
    private val refreshIntentPublisher = PublishSubject.create<TasksIntent.RefreshIntent>()
    private val clearCompletedTaskIntentPublisher =
        PublishSubject.create<TasksIntent.ClearCompletedTasksIntent>()
    private val changeFilterIntentPublisher = PublishSubject.create<TasksIntent.ChangeFilterIntent>()
    private val disposables = CompositeDisposable()
    private val viewModel: TasksViewModel by lazy(NONE) {
        ViewModelProviders
            .of(this, ToDoViewModelFactory.getInstance(context!!))
            .get(TasksViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        listAdapter = TasksAdapter(ArrayList(0))
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        bind()
    }

    private fun bind() {
        disposables.add(viewModel.states().subscribe(this::render))
        viewModel.processIntents(intents())
    }

    override fun onResume() {
        super.onResume()
        refreshIntentPublisher.onNext(TasksIntent.RefreshIntent(false))
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.dispose()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_tasks, container, false)

        val listView = root.findViewById<ListView>(R.id.tasks_list)
        listView.adapter = listAdapter
        filteringLabelView = root.findViewById(R.id.filteringLabel)
        tasksView = root.findViewById(R.id.tasksLL)

        noTasksView = root.findViewById(R.id.noTasks)
        noTaskIcon = root.findViewById(R.id.noTasksIcon)
        noTaskMainView = root.findViewById(R.id.noTasksMain)
        noTaskAddView = root.findViewById(R.id.noTasksAdd)

        swipeRefreshLayout = root.findViewById(R.id.refresh_layout)
        swipeRefreshLayout.setColorSchemeColors(
            ContextCompat.getColor(activity!!, R.color.colorPrimary),
            ContextCompat.getColor(activity!!, R.color.colorAccent),
            ContextCompat.getColor(activity!!, R.color.colorPrimaryDark)
        )

        swipeRefreshLayout.setScrollUpChild(listView)

        setHasOptionsMenu(true)

        return root
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item!!.itemId) {
            R.id.menu_clear ->
                clearCompletedTaskIntentPublisher.onNext(TasksIntent.ClearCompletedTasksIntent)
            R.id.menu_filter -> showFilteringPopUpMenu()
            R.id.menu_refresh -> refreshIntentPublisher.onNext(TasksIntent.RefreshIntent(true))
        }
        return true
    }

    override fun onCreateOptionsMenu(
        menu: Menu,
        inflater: MenuInflater
    ) {
        inflater.inflate(R.menu.tasks_fragment_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun intents(): Observable<TasksIntent> {
        return Observable.merge(
            initialIntent(),
            refreshIntent(),
            adapterIntents(),
            clearCompletedTaskIntent()
        )
            .mergeWith(
                changeFilterIntent()
            )
    }

    override fun render(state: TasksViewState) {
        swipeRefreshLayout.isRefreshing = state.isLoading
        if (state.error != null) {
            showLoadingTasksError()
            return
        }

        when (state.uiNotification) {
            TASK_COMPLETE -> showMessage(getString(R.string.task_marked_complete))
            TASK_ACTIVATED -> showMessage(getString(R.string.task_marked_active))
            COMPLETE_TASKS_CLEARED -> showMessage(getString(R.string.completed_tasks_cleared))
            null -> {
            }
        }

        if (state.tasks.isEmpty()) {
            when (state.tasksFilterType) {
                ACTIVE_TASKS -> showNoActiveTasks()
                COMPLETED_TASKS -> showNoCompletedTasks()
                else -> showNoTasks()
            }
        } else {
            listAdapter.replaceData(state.tasks)

            tasksView.visibility = View.VISIBLE
            noTasksView.visibility = View.GONE

            when (state.tasksFilterType) {
                ACTIVE_TASKS -> showActiveFilterLabel()
                COMPLETED_TASKS -> showCompletedFilterLabel()
                else -> showAllFilterLabel()
            }
        }
    }

    private fun showFilteringPopUpMenu() {
        val popup = PopupMenu(context!!, activity!!.findViewById(R.id.menu_filter))
        popup.menuInflater.inflate(R.menu.filter_tasks, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.active -> changeFilterIntentPublisher.onNext(
                    TasksIntent.ChangeFilterIntent(ACTIVE_TASKS)
                )
                R.id.completed -> changeFilterIntentPublisher.onNext(
                    TasksIntent.ChangeFilterIntent(COMPLETED_TASKS)
                )
                else -> changeFilterIntentPublisher.onNext(TasksIntent.ChangeFilterIntent(
                    ALL_TASKS
                ))
            }
            true
        }

        popup.show()
    }

    private fun showMessage(message: String) {
        val view = view ?: return
        Snackbar.make(view, message, Snackbar.LENGTH_LONG)
            .show()
    }

    private fun initialIntent(): Observable<TasksIntent.InitialIntent> {
        return Observable.just(TasksIntent.InitialIntent)
    }

    private fun refreshIntent(): Observable<TasksIntent.RefreshIntent> {
        return RxSwipeRefreshLayout.refreshes(swipeRefreshLayout)
            .map { TasksIntent.RefreshIntent(false) }
            .mergeWith(refreshIntentPublisher)
    }

    private fun clearCompletedTaskIntent(): Observable<TasksIntent.ClearCompletedTasksIntent> {
        return clearCompletedTaskIntentPublisher
    }

    private fun changeFilterIntent(): Observable<TasksIntent.ChangeFilterIntent> {
        return changeFilterIntentPublisher
    }

    private fun adapterIntents(): Observable<TasksIntent> {
        return listAdapter.taskToggleObservable.map { task ->
            if (!task.completed) {
                CompleteTaskIntent(task)
            } else {
                ActivateTaskIntent(task)
            }
        }
    }

    private fun showNoActiveTasks() {
        showNoTasksViews(
            resources.getString(R.string.no_tasks_active),
            R.drawable.ic_check_circle_24dp, false
        )
    }

    private fun showNoTasks() {
        showNoTasksViews(
            resources.getString(R.string.no_tasks_all),
            R.drawable.ic_assignment_turned_in_24dp, true
        )
    }

    private fun showNoCompletedTasks() {
        showNoTasksViews(
            resources.getString(R.string.no_tasks_completed),
            R.drawable.ic_verified_user_24dp, false
        )
    }

    private fun showNoTasksViews(
        mainText: String,
        iconRes: Int,
        showAddView: Boolean
    ) {
        tasksView.visibility = View.GONE
        noTasksView.visibility = View.VISIBLE

        noTaskMainView.text = mainText
        noTaskIcon.setImageDrawable(ContextCompat.getDrawable(context!!, iconRes))
        noTaskAddView.visibility = if (showAddView) View.VISIBLE else View.GONE
    }

    private fun showActiveFilterLabel() {
        filteringLabelView.text = resources.getString(R.string.label_active)
    }

    private fun showCompletedFilterLabel() {
        filteringLabelView.text = resources.getString(R.string.label_completed)
    }

    private fun showAllFilterLabel() {
        filteringLabelView.text = resources.getString(R.string.label_all)
    }

    private fun showLoadingTasksError() {
        showMessage(getString(R.string.loading_tasks_error))
    }

    companion object {
        operator fun invoke(): TasksFragment = TasksFragment()
    }
}
