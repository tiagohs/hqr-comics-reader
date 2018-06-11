package com.tiagohs.hqr.ui.callbacks

import android.view.animation.Animation

open class ISimpleAnimationListener : Animation.AnimationListener {
    override fun onAnimationRepeat(animation: Animation) {}

    override fun onAnimationEnd(animation: Animation) {}

    override fun onAnimationStart(animation: Animation) {}
}