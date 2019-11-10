package de.danoeh.antennapod.adapter

import android.content.Context
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.*
import de.danoeh.antennapod.R
import de.danoeh.antennapod.activity.MainActivity
import de.danoeh.antennapod.core.feed.Feed
import de.danoeh.antennapod.core.storage.FeedRepository
import de.danoeh.antennapod.core.util.LongIntMap
import de.danoeh.antennapod.fragment.FeedItemlistFragment
import de.danoeh.antennapod.fragment.SubscriptionItemMoveListener
import jp.shts.android.library.TriangleLabelView
import java.lang.ref.WeakReference

/**
 * Adapter for subscriptions
 */
class SubscriptionsAdapter(context: Context, mainActivity: MainActivity, val subscriptionItemMoveListener: SubscriptionItemMoveListener) : RecyclerView.Adapter<SubscriptionsAdapter.ViewHolder>() {
    private val mainActivityRef: WeakReference<MainActivity> = WeakReference(mainActivity)
    private val differ: AsyncListDiffer<Feed> = AsyncListDiffer(AdapterListUpdateCallback(this), AsyncDifferConfig.Builder<Feed>(object : DiffUtil.ItemCallback<Feed>(){
        override fun areItemsTheSame(oldItem: Feed, newItem: Feed): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Feed, newItem: Feed): Boolean {
            return oldItem == newItem
        }
    }).build())

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    var subscriptionData = FeedRepository.SubscriptionData(listOf(), LongIntMap())
        set(value) {
            field = value
            differ.submitList(value.feeds)
        }

    fun onItemMove(source: Int, target: Int) {
        subscriptionItemMoveListener.onItemMove(source, target)
        notifyItemMoved(source, target)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = inflater.inflate(R.layout.subscription_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return subscriptionData.feeds.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val feed = subscriptionData.feeds[position]
        holder.mainActivityRef = mainActivityRef
        holder.id = feed.id
        holder.feedTitle.text = feed.title
        holder.imageView.contentDescription = feed.title
        holder.feedTitle.visibility = View.VISIBLE
        val count = subscriptionData.feedCounters[feed.id]
        if(count > 0) {
            holder.count.primaryText = count.toString()
            holder.count.visibility = View.VISIBLE
        } else {
            holder.count.visibility = View.GONE
        }
        CoverLoader(mainActivityRef.get()).withUri(feed.imageLocation).withPlaceholderView(holder.feedTitle).withCoverView(holder.imageView).withError(R.color.light_gray).load()
    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnCreateContextMenuListener {
        var id: Long = 0
        var feedTitle: TextView
        var imageView: ImageView
        var count: TriangleLabelView
        var mainActivityRef: WeakReference<MainActivity>?

        init {
            itemView.setOnClickListener(this)
            feedTitle = itemView.findViewById(R.id.txtvTitle)
            imageView = itemView.findViewById(R.id.imgvCover)
            count = itemView.findViewById(R.id.triangleCountView)
            mainActivityRef = null
            itemView.setOnCreateContextMenuListener(this)
        }

        override fun onClick(v: View?) {
            val fragment: Fragment = FeedItemlistFragment.newInstance(id)
            mainActivityRef!!.get()!!.loadChildFragment(fragment)
        }

        override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
            menu!!.setHeaderTitle(feedTitle.text)
        }
    }
}