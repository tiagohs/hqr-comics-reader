package com.tiagohs.hqr.helpers.tools

import android.support.design.widget.AppBarLayout

abstract class AppBarMovieListener: AppBarLayout.OnOffsetChangedListener {

    enum class State {
        EXPANDED,
        COLLAPSED,
        IDLE
    }

    private var mCurrentState = State.IDLE

    override fun onOffsetChanged(appBarLayout: AppBarLayout, i: Int) {
        if (i == 0) {
            if (mCurrentState != State.EXPANDED) {
                onExpanded(appBarLayout)
            }
            mCurrentState = State.EXPANDED
        } else if (Math.abs(i) >= appBarLayout.totalScrollRange) {
            if (mCurrentState != State.COLLAPSED) {
                onCollapsed(appBarLayout)
            }
            mCurrentState = State.COLLAPSED
        } else {
            if (mCurrentState != State.IDLE) {
                onIdle(appBarLayout)
            }
            mCurrentState = State.IDLE
        }
    }

    abstract fun onExpanded(appBarLayout: AppBarLayout)
    abstract fun onCollapsed(appBarLayout: AppBarLayout)
    abstract fun onIdle(appBarLayout: AppBarLayout)
}