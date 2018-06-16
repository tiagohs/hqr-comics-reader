package com.tiagohs.hqr.helpers.tools

import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView

abstract class EndlessRecyclerView : RecyclerView.OnScrollListener {

    private var previousTotal = 0 // The total number of items in the dataset after the last load
    private var loading = true // True if we are still waiting for the last set of data to load.
    private var visibleThreshold = 5 // The minimum amount of items to have below your current scroll position before loading more.
    internal var firstVisibleItem: Int = 0
    internal var visibleItemCount: Int = 0
    internal var totalItemCount: Int = 0

    private var current_page = 1

    private var mLayoutManager: RecyclerView.LayoutManager? = null

    constructor(layoutManager: RecyclerView.LayoutManager) {
        this.mLayoutManager = layoutManager
    }

    constructor(layoutManager: GridLayoutManager) {
        this.mLayoutManager = layoutManager
        // Increase visible threshold based on number of columns
        visibleThreshold = visibleThreshold * layoutManager.spanCount
    }

    override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        visibleItemCount = recyclerView!!.childCount
        totalItemCount = mLayoutManager!!.itemCount

        // Check the layout manager type in order to determine the last visible position
        if (mLayoutManager is LinearLayoutManager) {

            firstVisibleItem = (mLayoutManager as LinearLayoutManager).findFirstVisibleItemPosition()

        } else if (mLayoutManager is GridLayoutManager) {

            firstVisibleItem = (mLayoutManager as GridLayoutManager).findFirstVisibleItemPosition()
        }


        if (loading) {
            if (totalItemCount > previousTotal) {
                loading = false
                previousTotal = totalItemCount
            }
        }
        if (!loading && totalItemCount - visibleItemCount <= firstVisibleItem + visibleThreshold) {
            // End has been reached

            // Do something
            current_page++

            onLoadMore(current_page)

            loading = true
        }
    }

    abstract fun onLoadMore(current_page: Int)

    companion object {

        var TAG = EndlessRecyclerView::class.java.simpleName
    }

}