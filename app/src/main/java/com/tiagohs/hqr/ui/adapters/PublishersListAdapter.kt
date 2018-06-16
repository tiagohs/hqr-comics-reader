package com.tiagohs.hqr.ui.adapters

import android.content.Context
import android.support.v7.widget.RecyclerView.Adapter
import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.tiagohs.hqr.R
import com.tiagohs.hqr.models.sources.Publisher
import com.tiagohs.hqr.ui.callbacks.IPublisherCallback
import com.tiagohs.hqr.helpers.utils.ScreenUtils
import kotlinx.android.synthetic.main.item_publisher.view.*
import kotlinx.android.synthetic.main.placeholder_image_rounded.view.*

class PublishersListAdapter(private val publishers: List<Publisher>,
                            private val context: Context?,
                            private val callback: IPublisherCallback) : Adapter<PublishersListAdapter.PublisherViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PublisherViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_publisher, parent, false)
        return PublisherViewHolder(view, callback, context)
    }

    override fun getItemCount(): Int {
        return publishers.size
    }

    override fun onBindViewHolder(holder: PublisherViewHolder, position: Int) {
        holder.onBindView(publishers[position])
    }

    class PublisherViewHolder(itemView: View) : ViewHolder(itemView), View.OnClickListener {

        lateinit var publisherTitle: TextView
        lateinit var publisherBackground: ImageView
        lateinit var placeholder: View

        lateinit var callback: IPublisherCallback
        lateinit var publisher: Publisher
        lateinit var context: Context


        constructor(itemView: View, callback: IPublisherCallback, context: Context?) : this(itemView) {
            this.callback = callback
            this.context = context!!

            itemView.setOnClickListener(this)

            publisherTitle = itemView.publisherTitle
            publisherBackground = itemView.publisherImgBack
            placeholder = itemView.placeholderItemPublisher
        }

        fun onBindView(publisher: Publisher) {
            this.publisher = publisher

            publisherTitle.text = publisher.name

            val backgroundDrawable = ScreenUtils.generateMaterialColorBackground(context, publisher.name, publisherBackground)
            publisherBackground.setImageDrawable(backgroundDrawable)
        }

        override fun onClick(p0: View?) {
            callback.onClick(publisher)
        }

    }
}

