package de.danoeh.antennapod.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import de.danoeh.antennapod.R
import de.danoeh.antennapod.activity.MainActivity
import de.danoeh.antennapod.core.feed.Feed
import de.danoeh.antennapod.fragment.FeedItemlistFragment
import jp.shts.android.library.TriangleLabelView
import java.lang.ref.WeakReference

/**
 * Adapter for subscriptions
 */
class SubscriptionsAdapter(mainActivity: MainActivity, itemAccess: ItemAccess) : BaseAdapter(), OnItemClickListener {
    private val mainActivityRef: WeakReference<MainActivity>
    private val itemAccess: ItemAccess
    override fun getCount(): Int {
        return itemAccess.count
    }

    override fun getItem(position: Int): Any? {
        return itemAccess.getItem(position)
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    override fun getItemId(position: Int): Long {
        return itemAccess.getItem(position)!!.id
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        val holder: Holder
        if (convertView == null) {
            holder = Holder()
            val layoutInflater = mainActivityRef.get()!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = layoutInflater.inflate(R.layout.subscription_item, parent, false)
            holder.feedTitle = convertView.findViewById(R.id.txtvTitle)
            holder.imageView = convertView.findViewById(R.id.imgvCover)
            holder.count = convertView.findViewById(R.id.triangleCountView)
            convertView.tag = holder
        } else {
            holder = convertView.tag as Holder
        }
        val feed = getItem(position) as Feed
        holder.feedTitle!!.text = feed.title
        holder.imageView!!.contentDescription = feed.title
        holder.feedTitle!!.visibility = View.VISIBLE
        val count = itemAccess.getFeedCounter(feed.id)
        if (count > 0) {
            holder.count!!.primaryText = itemAccess.getFeedCounter(feed.id).toString()
            holder.count!!.visibility = View.VISIBLE
        } else {
            holder.count!!.visibility = View.GONE
        }
        CoverLoader(mainActivityRef.get())
                .withUri(feed.imageLocation)
                .withPlaceholderView(holder.feedTitle)
                .withCoverView(holder.imageView)
                .withError(R.color.light_gray)
                .load()
        return convertView!!
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
        val fragment: Fragment = FeedItemlistFragment.newInstance(getItemId(position))
        mainActivityRef.get()!!.loadChildFragment(fragment)
    }

    internal class Holder {
        var feedTitle: TextView? = null
        var imageView: ImageView? = null
        var count: TriangleLabelView? = null
    }

    interface ItemAccess {
        val count: Int
        fun getItem(position: Int): Feed?
        fun getFeedCounter(feedId: Long): Int
    }

    companion object {
        /** placeholder object that indicates item should be added  */
        val ADD_ITEM_OBJ = Any()
        /** the position in the view that holds the add item; 0 is the first, -1 is the last position  */
        private const val TAG = "SubscriptionsAdapter"
    }

    init {
        mainActivityRef = WeakReference(mainActivity)
        this.itemAccess = itemAccess
    }
}