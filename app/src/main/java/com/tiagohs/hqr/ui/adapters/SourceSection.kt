package com.tiagohs.hqr.ui.adapters

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.tiagohs.hqr.R
import com.tiagohs.hqr.helpers.extensions.getResourceColor
import com.tiagohs.hqr.helpers.tools.PreferenceHelper
import com.tiagohs.hqr.helpers.tools.getOrDefault
import com.tiagohs.hqr.helpers.utils.LocaleUtils
import com.tiagohs.hqr.models.database.SourceDB
import com.tiagohs.hqr.ui.callbacks.ISourcesCallback
import com.tiagohs.hqr.ui.views.config.BaseActivity
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters
import io.github.luizgrp.sectionedrecyclerviewadapter.StatelessSection
import javax.inject.Inject

class SourceSection(
        val context: Context,
        val sectionName: String?,
        val items: List<SourceDB>?,
        val callback: ISourcesCallback
) : StatelessSection(SectionParameters.builder()
                                        .itemResourceId(R.layout.item_content_source_section)
                                        .headerResourceId(R.layout.item_header_source_section)
                                        .build()) {

    @Inject
    lateinit var preferenceHelper: PreferenceHelper

    @Inject
    lateinit var localeUtils: LocaleUtils

    init {
        (context as BaseActivity).getApplicationComponent()?.inject(this)
    }

    override fun getContentItemsTotal(): Int = items!!.size

    override fun onBindItemViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        (holder as SectionViewHolder).onBindItem(items!!.get(position))
    }

    override fun onBindHeaderViewHolder(holder: RecyclerView.ViewHolder?) {
        (holder as SectionHeaderViewHolder).onBindItem(sectionName)
    }

    override fun getHeaderViewHolder(view: View?): RecyclerView.ViewHolder {
        return SectionHeaderViewHolder(view, context, localeUtils)
    }

    override fun getItemViewHolder(view: View?): RecyclerView.ViewHolder {
        return SectionViewHolder(context, view, preferenceHelper, callback)
    }

    internal class SectionViewHolder(itemView: View?): RecyclerView.ViewHolder(itemView), View.OnClickListener {

        private lateinit var mSourceDB: SourceDB
        private lateinit var callback: ISourcesCallback
        private lateinit var context: Context

        private lateinit var sourceName: TextView
        private lateinit var sourceUrl: TextView
        private lateinit var checkIcon: ImageView

        private lateinit var preferenceHelper: PreferenceHelper

        private var isCurrentSourceSelect: Boolean = false

        constructor(context: Context, itemView: View?, preferenceHelper: PreferenceHelper,callback: ISourcesCallback) : this(itemView) {
            this.callback = callback
            this.preferenceHelper = preferenceHelper
            this.context = context

            itemView?.setOnClickListener(this)

            this.sourceName = itemView?.findViewById(R.id.sourceName) as TextView
            this.sourceUrl = itemView.findViewById(R.id.sourceUrl) as TextView
            this.checkIcon = itemView.findViewById(R.id.checkIcon) as ImageView
        }

        fun onBindItem(sourceDB: SourceDB) {
            this.mSourceDB = sourceDB
            this.isCurrentSourceSelect = sourceDB.isCurrentSelect(preferenceHelper.currentSource().getOrDefault())

            sourceName.text = sourceDB.name
            sourceUrl.text = sourceDB.baseUrl

            if (isCurrentSourceSelect) {
                sourceName.setTextColor(context.getResourceColor(R.color.colorAccent))
                sourceUrl.setTextColor(context.getResourceColor(R.color.colorAccent))
                checkIcon.visibility = View.VISIBLE
            }
        }

        override fun onClick(p0: View?) {
            preferenceHelper.currentSource().set(mSourceDB.id)

            callback.onSourceSelect(mSourceDB)
        }
    }

    internal class SectionHeaderViewHolder(
            itemView: View?,
            val context: Context?,
            val localeUtils: LocaleUtils): RecyclerView.ViewHolder(itemView) {
        private var sourceLanguage: TextView = itemView?.findViewById(R.id.sourceLanguage) as TextView

        fun onBindItem(sectionName: String?) {
            sourceLanguage.text = localeUtils.getDisplayName(sectionName, context!!)
        }

    }
}