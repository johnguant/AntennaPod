package de.danoeh.antennapod.view

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.RelativeLayout.LayoutParams
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import de.danoeh.antennapod.R

class EmptyViewHandler(context: Context) {
    private var layoutAdded = false
    private var recyclerView: RecyclerView? = null
    private var adapter: Adapter<*>? = null
    private val context: Context
    private val emptyView: View
    private val tvTitle: TextView
    private val tvMessage: TextView
    private val ivIcon: ImageView
    fun setTitle(title: Int) {
        tvTitle.setText(title)
    }

    fun setMessage(message: Int) {
        tvMessage.setText(message)
    }

    fun setIcon(@AttrRes iconAttr: Int) {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(iconAttr, typedValue, true)
        val d = ContextCompat.getDrawable(context, typedValue.resourceId)
        ivIcon.setImageDrawable(d)
        ivIcon.visibility = View.VISIBLE
    }

    fun hide() {
        emptyView.visibility = View.GONE
    }

    fun attachToListView(listView: AbsListView) {
        check(!layoutAdded) { "Can not attach EmptyView multiple times" }
        addToParentView(listView)
        layoutAdded = true
        listView.emptyView = emptyView
    }

    fun attachToRecyclerView(recyclerView: RecyclerView) {
        check(!layoutAdded) { "Can not attach EmptyView multiple times" }
        addToParentView(recyclerView)
        layoutAdded = true
        this.recyclerView = recyclerView
        updateAdapter(recyclerView.adapter)
    }

    private fun addToParentView(view: View) {
        val parent = view.parent as ViewGroup
        parent.addView(emptyView)
        if (parent is RelativeLayout) {
            val layoutParams = emptyView.layoutParams as LayoutParams
            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
            emptyView.layoutParams = layoutParams
        }
    }

    fun updateAdapter(adapter: Adapter<*>?) {
        if (this.adapter != null) {
            this.adapter!!.unregisterAdapterDataObserver(adapterObserver)
        }

        this.adapter = adapter
        adapter?.registerAdapterDataObserver(adapterObserver)
        updateVisibility()
    }

    private val adapterObserver: SimpleAdapterDataObserver = object : SimpleAdapterDataObserver() {
        override fun anythingChanged() {
            updateVisibility()
        }
    }

    fun updateVisibility() {
        val empty: Boolean = if (adapter == null) {
            true
        } else {
            adapter!!.itemCount == 0
        }
        if (recyclerView != null) {
            recyclerView!!.visibility = if (empty) View.GONE else View.VISIBLE
        }
        emptyView.visibility = if (empty) View.VISIBLE else View.GONE
    }

    init {
        emptyView = View.inflate(context, R.layout.empty_view_layout, null)
        this.context = context
        tvTitle = emptyView.findViewById(R.id.emptyViewTitle)
        tvMessage = emptyView.findViewById(R.id.emptyViewMessage)
        ivIcon = emptyView.findViewById(R.id.emptyViewIcon)
    }
}