package com.tiagohs.hqr.ui.adapters

import android.content.Context
import android.support.v7.widget.RecyclerView.Adapter
import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tiagohs.hqr.R
import com.tiagohs.hqr.models.sources.Publisher
import com.tiagohs.hqr.utils.ScreenUtils
import kotlinx.android.synthetic.main.item_publisher.view.*

class PublishersListAdapter(private val publishers: List<Publisher>,
                            private val context: Context?) : Adapter<PublishersListAdapter.PublisherViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PublisherViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_publisher, parent, false)
        return PublisherViewHolder(view)
    }

    override fun getItemCount(): Int {
        return publishers.size
    }

    override fun onBindViewHolder(holder: PublisherViewHolder, position: Int) {
        val publisher = publishers[position]

        holder.publisherTitle.text = publisher.name

        val backgroundDrawable = ScreenUtils.generateMaterialColorBackground(context)
        holder.publisherBackground.setImageDrawable(backgroundDrawable)
    }

    class PublisherViewHolder(itemView: View) : ViewHolder(itemView) {

        val publisherTitle = itemView.publisherTitle
        val publisherBackground = itemView.publisherImgBack
    }
}

