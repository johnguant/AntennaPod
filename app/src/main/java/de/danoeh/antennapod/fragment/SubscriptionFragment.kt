package de.danoeh.antennapod.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.ContextMenu.ContextMenuInfo
import android.widget.AdapterView.AdapterContextMenuInfo
import android.widget.GridView
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import de.danoeh.antennapod.R
import de.danoeh.antennapod.activity.MainActivity
import de.danoeh.antennapod.adapter.SubscriptionsAdapter
import de.danoeh.antennapod.adapter.SubscriptionsAdapter.ItemAccess
import de.danoeh.antennapod.core.asynctask.FeedRemover
import de.danoeh.antennapod.core.dialog.ConfirmationDialog
import de.danoeh.antennapod.core.event.DownloadEvent
import de.danoeh.antennapod.core.event.FeedListUpdateEvent
import de.danoeh.antennapod.core.event.UnreadItemsUpdateEvent
import de.danoeh.antennapod.core.feed.Feed
import de.danoeh.antennapod.core.menuhandler.MenuItemUtils.UpdateRefreshMenuItemChecker
import de.danoeh.antennapod.core.preferences.PlaybackPreferences
import de.danoeh.antennapod.core.service.download.DownloadService
import de.danoeh.antennapod.core.service.playback.PlaybackService
import de.danoeh.antennapod.core.storage.DBReader
import de.danoeh.antennapod.core.storage.DBReader.NavDrawerData
import de.danoeh.antennapod.core.storage.DBWriter
import de.danoeh.antennapod.core.storage.DownloadRequester
import de.danoeh.antennapod.core.util.FeedItemUtil
import de.danoeh.antennapod.core.util.IntentUtils
import de.danoeh.antennapod.core.util.download.AutoUpdateManager
import de.danoeh.antennapod.dialog.RenameFeedDialog
import de.danoeh.antennapod.menuhandler.MenuItemUtils
import de.danoeh.antennapod.view.EmptyViewHandler
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.concurrent.Callable
import java.util.concurrent.Future

/**
 * Fragment for displaying feed subscriptions
 */
class SubscriptionFragment : Fragment() {
    private lateinit var subscriptionGridLayout: GridView
    private var navDrawerData: NavDrawerData? = null
    private lateinit var subscriptionAdapter: SubscriptionsAdapter
    private lateinit var subscriptionAddButton: FloatingActionButton
    private lateinit var emptyView: EmptyViewHandler
    private var mPosition = -1
    private var isUpdatingFeeds = false
    private var disposable: Disposable? = null
    private lateinit var prefs: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        setHasOptionsMenu(true)
        prefs = requireActivity().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_subscriptions, container, false)
        subscriptionGridLayout = root.findViewById(R.id.subscriptions_grid)
        subscriptionGridLayout.numColumns = prefs.getInt(PREF_NUM_COLUMNS, 3)
        registerForContextMenu(subscriptionGridLayout)
        subscriptionAddButton = root.findViewById(R.id.subscriptions_add)
        setupEmptyView()
        return root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.subscriptions, menu)
        val columns = prefs.getInt(PREF_NUM_COLUMNS, 3)
        menu.findItem(R.id.subscription_num_columns_2).isChecked = columns == 2
        menu.findItem(R.id.subscription_num_columns_3).isChecked = columns == 3
        menu.findItem(R.id.subscription_num_columns_4).isChecked = columns == 4
        menu.findItem(R.id.subscription_num_columns_5).isChecked = columns == 5
        isUpdatingFeeds = MenuItemUtils.updateRefreshMenuItem(menu, R.id.refresh_item, updateRefreshMenuItemChecker)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (super.onOptionsItemSelected(item)) {
            true
        } else when (item.itemId) {
            R.id.refresh_item -> {
                AutoUpdateManager.runImmediate(requireContext())
                true
            }
            R.id.subscription_num_columns_2 -> {
                setColumnNumber(2)
                true
            }
            R.id.subscription_num_columns_3 -> {
                setColumnNumber(3)
                true
            }
            R.id.subscription_num_columns_4 -> {
                setColumnNumber(4)
                true
            }
            R.id.subscription_num_columns_5 -> {
                setColumnNumber(5)
                true
            }
            else -> false
        }
    }

    private fun setColumnNumber(columns: Int) {
        subscriptionGridLayout.numColumns = columns
        prefs.edit().putInt(PREF_NUM_COLUMNS, columns).apply()
        activity!!.invalidateOptionsMenu()
    }

    private fun setupEmptyView() {
        emptyView = EmptyViewHandler(context!!)
        emptyView.setIcon(R.attr.ic_folder)
        emptyView.setTitle(R.string.no_subscriptions_head_label)
        emptyView.setMessage(R.string.no_subscriptions_label)
        emptyView.attachToListView(subscriptionGridLayout)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        subscriptionAdapter = SubscriptionsAdapter(activity as MainActivity, itemAccess)
        subscriptionGridLayout.adapter = subscriptionAdapter
        subscriptionGridLayout.onItemClickListener = subscriptionAdapter
        subscriptionAddButton.setOnClickListener {
            if (activity is MainActivity) {
                (activity as MainActivity?)!!.loadChildFragment(AddFeedFragment())
            }
        }
        if (activity is MainActivity) {
            (activity as MainActivity?)!!.supportActionBar!!.setTitle(R.string.subscriptions_label)
        }
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
        loadSubscriptions()
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
        if (disposable != null) {
            disposable!!.dispose()
        }
    }

    private fun loadSubscriptions() {
        if (disposable != null) {
            disposable!!.dispose()
        }
        emptyView.hide()
        disposable = Observable.fromCallable { DBReader.getNavDrawerData() }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ result: NavDrawerData? ->
                    navDrawerData = result
                    subscriptionAdapter.notifyDataSetChanged()
                    emptyView.updateVisibility()
                }) { error: Throwable? -> Log.e(TAG, Log.getStackTraceString(error)) }
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val adapterInfo = menuInfo as AdapterContextMenuInfo?
        val position = adapterInfo!!.position
        val selectedObject = subscriptionAdapter.getItem(position)
        if (selectedObject == SubscriptionsAdapter.ADD_ITEM_OBJ) {
            mPosition = position
            return
        }
        val feed = selectedObject as Feed
        val inflater = requireActivity().menuInflater
        inflater.inflate(R.menu.nav_feed_context, menu)
        menu.setHeaderTitle(feed.title)
        mPosition = position
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val position = mPosition
        mPosition = -1 // reset
        if (position < 0) {
            return false
        }
        val selectedObject = subscriptionAdapter.getItem(position)
        if (selectedObject == SubscriptionsAdapter.ADD_ITEM_OBJ) { // this is the add object, do nothing
            return false
        }
        val feed = selectedObject as Feed
        return when (item.itemId) {
            R.id.remove_all_new_flags_item -> {
                displayConfirmationDialog(
                        R.string.remove_all_new_flags_label,
                        R.string.remove_all_new_flags_confirmation_msg,
                        Callable<Future<Any>?> { DBWriter.removeFeedNewFlag(feed.id) as Future<Any>? })
                true
            }
            R.id.mark_all_read_item -> {
                displayConfirmationDialog(
                        R.string.mark_all_read_label,
                        R.string.mark_all_read_confirmation_msg,
                        Callable<Future<Any>?> { DBWriter.markFeedRead(feed.id) as Future<Any>? })
                true
            }
            R.id.rename_item -> {
                RenameFeedDialog(activity, feed).show()
                true
            }
            R.id.remove_item -> {
                displayRemoveFeedDialog(feed)
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    private fun displayRemoveFeedDialog(feed: Feed) {
        val remover: FeedRemover = object : FeedRemover(context, feed) {
            override fun onPostExecute(result: Void?) {
                super.onPostExecute(result)
                loadSubscriptions()
            }
        }
        val message = getString(R.string.feed_delete_confirmation_msg, feed.title)
        val dialog: ConfirmationDialog = object : ConfirmationDialog(context, R.string.remove_feed_label, message) {
            override fun onConfirmButtonPressed(clickedDialog: DialogInterface) {
                clickedDialog.dismiss()
                val mediaId = PlaybackPreferences.getCurrentlyPlayingFeedMediaId()
                if (mediaId > 0 && FeedItemUtil.indexOfItemWithMediaId(feed.items, mediaId) >= 0) {
                    Log.d(TAG, "Currently playing episode is about to be deleted, skipping")
                    remover.skipOnCompletion = true
                    val playerStatus = PlaybackPreferences.getCurrentPlayerStatus()
                    if (playerStatus == PlaybackPreferences.PLAYER_STATUS_PLAYING) {
                        IntentUtils.sendLocalBroadcast(context, PlaybackService.ACTION_PAUSE_PLAY_CURRENT_EPISODE)
                    }
                }
                remover.executeAsync()
            }
        }
        dialog.createNewDialog().show()
    }

    private fun <T> displayConfirmationDialog(@StringRes title: Int, @StringRes message: Int, task: Callable<out T?>) {
        val dialog: ConfirmationDialog = object : ConfirmationDialog(activity, title, message) {
            @SuppressLint("CheckResult")
            override fun onConfirmButtonPressed(clickedDialog: DialogInterface) {
                clickedDialog.dismiss()
                Observable.fromCallable(task)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ loadSubscriptions() },
                                { error: Throwable? -> Log.e(TAG, Log.getStackTraceString(error)) })
            }
        }
        dialog.createNewDialog().show()
    }

    @Subscribe
    fun onFeedListChanged(event: FeedListUpdateEvent?) {
        loadSubscriptions()
    }

    @Subscribe
    fun onUnreadItemsChanged(event: UnreadItemsUpdateEvent?) {
        loadSubscriptions()
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: DownloadEvent) {
        Log.d(TAG, "onEventMainThread() called with: event = [$event]")
        if (event.hasChangedFeedUpdateStatus(isUpdatingFeeds)) {
            activity!!.invalidateOptionsMenu()
        }
    }

    private val updateRefreshMenuItemChecker = UpdateRefreshMenuItemChecker { DownloadService.isRunning && DownloadRequester.getInstance().isDownloadingFeeds }
    private val itemAccess: ItemAccess = object : ItemAccess {

        override val count: Int
            get() {
                return if (navDrawerData != null) {
                    navDrawerData!!.feeds.size
                } else {
                    0
                }
            }

        override fun getItem(position: Int): Feed? {
            return if (navDrawerData != null && 0 <= position && position < navDrawerData!!.feeds.size) {
                navDrawerData!!.feeds[position]
            } else {
                null
            }
        }

        override fun getFeedCounter(feedId: Long): Int {
            return if (navDrawerData != null) navDrawerData!!.feedCounters[feedId] else 0
        }
    }

    companion object {
        const val TAG = "SubscriptionFragment"
        private const val PREFS = "SubscriptionFragment"
        private const val PREF_NUM_COLUMNS = "columns"
    }
}