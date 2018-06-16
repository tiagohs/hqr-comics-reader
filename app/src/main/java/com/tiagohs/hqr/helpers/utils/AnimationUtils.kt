package com.tiagohs.hqr.helpers.utils

import android.animation.Animator
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.ScaleAnimation

class AnimationUtils {

    companion object {
        fun createFadeInAnimation(duration: Int): Animation {
            val fadeIn = AlphaAnimation(0f, 1f)
            fadeIn.interpolator = DecelerateInterpolator()
            fadeIn.duration = duration.toLong()

            return fadeIn
        }

        fun createFadeOutAnimation(duration: Int): Animation {
            val fadeIn = AlphaAnimation(1f, 0f)
            fadeIn.interpolator = DecelerateInterpolator()
            fadeIn.duration = duration.toLong()

            return fadeIn
        }

        fun createShowCircularReveal(view: View) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                val cx = (view.left + view.right) / 2
                val cy = (view.top + view.bottom) / 2
                val finalRadius = Math.max(view.width, view.height)

                val anim = ViewAnimationUtils.createCircularReveal(view, cx, cy, 0f, finalRadius.toFloat())
                anim.start()
            } else
                view.visibility = View.VISIBLE
        }

        fun createShowCircularReveal(view: View, listener: Animator.AnimatorListener) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                val cx = (view.left + view.right) / 2
                val cy = (view.top + view.bottom) / 2
                val finalRadius = Math.max(view.width, view.height)

                val anim = ViewAnimationUtils.createCircularReveal(view, cx, cy, 0f, finalRadius.toFloat())
                anim.addListener(listener)
                anim.start()
            } else
                view.visibility = View.VISIBLE
        }

        fun createHideCircularReveal(view: View) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                val cx = (view.left + view.right) / 2
                val cy = (view.top + view.bottom) / 2
                val startRadius = Math.max(view.width, view.height)

                val anim = ViewAnimationUtils.createCircularReveal(view, cx, cy, startRadius.toFloat(), 0f)
                anim.start()
            } else
                view.visibility = View.GONE
        }

        fun createHideCircularReveal(view: View, listener: Animator.AnimatorListener) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                val cx = (view.left + view.right) / 2
                val cy = (view.top + view.bottom) / 2
                val startRadius = Math.max(view.width, view.height)

                val anim = ViewAnimationUtils.createCircularReveal(view, cx, cy, startRadius.toFloat(), 0f)
                anim.addListener(listener)
                anim.start()
            } else
                view.visibility = View.GONE
        }

        fun creatScaleUpAnimation(view: View, duration: Int) {

            val fade_in = ScaleAnimation(0f, 1f, 0f, 1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
            fade_in.duration = duration.toLong()     // animation duration in milliseconds
            fade_in.fillAfter = true    // If fillAfter is true, the transformation that this animation performed will persist when it is finished.
            view.startAnimation(fade_in)

        }
    }
}