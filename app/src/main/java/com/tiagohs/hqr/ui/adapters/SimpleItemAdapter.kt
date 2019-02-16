package com.tiagohs.hqr.ui.adapters

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tiagohs.hqr.R
import com.tiagohs.hqr.models.view_models.DefaultModelView
import com.tiagohs.hqr.ui.callbacks.ISimpleItemCallback
import kotlinx.android.synthetic.main.item_simple_item.view.*

class SimpleItemAdapter(private val items: List<DefaultModelView>?,
                        private val context: Context?,
                        private val callback: ISimpleItemCallback) : RecyclerView.Adapter<SimpleItemAdapter.SimpleItemrViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleItemrViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_simple_item, parent, false)
        return SimpleItemrViewHolder(view, callback)
    }

    override fun getItemCount(): Int {
        return items?.size ?: 0
    }

    override fun onBindViewHolder(holder: SimpleItemrViewHolder, position: Int) {
        val items = items ?: return

        holder.onBindItem(items[position])
    }

    class SimpleItemrViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        lateinit var callback: ISimpleItemCallback
        lateinit var item: DefaultModelView

        val button = itemView.item

        constructor(itemView: View, callback: ISimpleItemCallback) : this(itemView) {
            this.callback = callback

            itemView.item.setOnClickListener(this)
        }

        fun onBindItem(item: DefaultModelView) {
            this.item = item

            button.text = item.name
        }

        override fun onClick(p0: View?) {
            callback.onClick(item)
        }
    }
}