package com.tiagohs.hqr.utils

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.view.View
import android.widget.ImageView
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import com.squareup.picasso.RequestCreator
import java.lang.Exception

class ImageUtils {

    companion object {

        fun load(imageView: ImageView, url: String?) {
            load(imageView, url, null, null, false)
        }

        fun loadWithRevealAnimation(context: Context, imageView: ImageView, url: String?, placeholderPath: Int?, errorPath: Int?) {

            val loader = Picasso.get()
            var request: RequestCreator? = null

            if (url != null)
                request = loader.load(url)

            if (placeholderPath != null)
                request!!.placeholder(placeholderPath)

            if (errorPath != null)
                request!!.error(errorPath)

            request!!.into(imageView, object: Callback {
                override fun onSuccess() {
                    if (!(context as Activity).isDestroyed) {
                        AnimationUtils.createShowCircularReveal(imageView)
                    }
                }

                override fun onError(e: Exception?) {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }
            })
        }

        fun load(imageView: ImageView, url: String?, placeholderPath: Int?, errorPath: Int?, isToFit: Boolean) {

            val loader = Picasso.get()
            var request: RequestCreator? = null

            if (url != null)
                request = loader.load(url)

            if (placeholderPath != null)
                request!!.placeholder(placeholderPath)

            if (errorPath != null)
                request!!.error(errorPath)

            if (isToFit)
                request!!.fit()

            request!!.into(imageView)
        }

        fun load(imageView: ImageView, url: String?, errorPath: Int?, isToFit: Boolean, placeholderView: View) {

            val loader = Picasso.get()
            var request: RequestCreator? = null

            if (url != null)
                request = loader.load(url)

            if (errorPath != null)
                request!!.error(errorPath)

            if (isToFit)
                request!!.fit()

            request!!.into(imageView, object : Callback {
                override fun onSuccess() {
                    placeholderView.visibility = View.GONE
                    imageView.visibility = View.VISIBLE
                }

                override fun onError(e: Exception?) {
                    placeholderView.visibility = View.GONE
                }

            })
        }

        fun load(context: Context?, pathImg: Int, comicsImage: ImageView?, pathPlaceholder: Int) {
            BitmapCreator(context).loadBitmap(pathImg, comicsImage, BitmapFactory.decodeResource(context!!.resources,
                    pathPlaceholder))
        }
    }
}