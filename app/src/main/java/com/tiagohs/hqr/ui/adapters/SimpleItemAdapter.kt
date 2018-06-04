package com.tiagohs.hqr.ui.adapters

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tiagohs.hqr.R
import com.tiagohs.hqr.models.sources.SimpleItem
import com.tiagohs.hqr.ui.callbacks.ISimpleItemCallback
import kotlinx.android.synthetic.main.item_simple_item.view.*

class SimpleItemAdapter(private val items: List<SimpleItem>?,
                        private val context: Context?,
                        private val callback: ISimpleItemCallback) : RecyclerView.Adapter<SimpleItemAdapter.SimpleItemrViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleItemrViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_simple_item, parent, false)
        return SimpleItemrViewHolder(view, callback)
    }

    override fun getItemCount(): Int {
        return items!!.size
    }

    override fun onBindViewHolder(holder: SimpleItemrViewHolder, position: Int) {
        holder.onBindItem(items!![position])
    }

    class SimpleItemrViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        lateinit var callback: ISimpleItemCallback
        lateinit var item: SimpleItem

        val button = itemView.item

        constructor(itemView: View, callback: ISimpleItemCallback) : this(itemView) {
            this.callback = callback

            itemView.setOnClickListener(this)
        }

        fun onBindItem(item: SimpleItem) {
            this.item = item

            button.text = item.title
        }

        override fun onClick(p0: View?) {
            callback.onClick(item)
        }
    }
}