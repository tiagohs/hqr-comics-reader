package com.tiagohs.hqr.ui.adapters

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import com.tiagohs.hqr.R
import com.tiagohs.hqr.helpers.utils.LocaleUtils
import com.tiagohs.hqr.models.database.Source
import com.tiagohs.hqr.ui.callbacks.ISourcesCallback
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters
import io.github.luizgrp.sectionedrecyclerviewadapter.StatelessSection

class SourceSection(
        val context: Context,
        val sectionName: String?,
        val items: List<Source>?,
        val callback: ISourcesCallback
) : StatelessSection(SectionParameters.builder()
                                        .itemResourceId(R.layout.item_content_source_section)
                                        .headerResourceId(R.layout.item_header_source_section)
                                        .build()) {

    override fun getContentItemsTotal(): Int = items!!.size

    override fun onBindItemViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        (holder as SectionViewHolder).onBindItem(items!!.get(position))
    }

    override fun onBindHeaderViewHolder(holder: RecyclerView.ViewHolder?) {
        (holder as SectionHeaderViewHolder).onBindItem(sectionName)
    }

    override fun getHeaderViewHolder(view: View?): RecyclerView.ViewHolder {
        return SectionHeaderViewHolder(view, context)
    }

    override fun getItemViewHolder(view: View?): RecyclerView.ViewHolder {
        return SectionViewHolder(view, callback)
    }

    internal class SectionViewHolder(itemView: View?): RecyclerView.ViewHolder(itemView), View.OnClickListener {

        private lateinit var source: Source
        private lateinit var callback: ISourcesCallback

        private lateinit var sourceName: TextView

        constructor(itemView: View?, callback: ISourcesCallback) : this(itemView) {
            this.callback = callback

            itemView?.setOnClickListener(this)

            this.sourceName = itemView?.findViewById(R.id.sourceName) as TextView
        }

        fun onBindItem(source: Source) {
            this.source = source

            sourceName.text = source.name
        }

        override fun onClick(p0: View?) {
            callback.onSourceSelect(source)
        }
    }

    internal class SectionHeaderViewHolder(
            itemView: View?,
            val context: Context?): RecyclerView.ViewHolder(itemView) {
        private var sourceLanguage: TextView = itemView?.findViewById(R.id.sourceLanguage) as TextView

        fun onBindItem(sourceName: String?) {
            sourceLanguage.text = LocaleUtils.getDisplayLanguageAndCountryName(context!!, sourceName!!)
        }

    }
}