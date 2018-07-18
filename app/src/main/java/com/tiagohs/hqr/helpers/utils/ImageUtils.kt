package com.tiagohs.hqr.helpers.utils

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.View
import android.widget.ImageView
import com.squareup.picasso.Callback
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.Picasso
import com.squareup.picasso.RequestCreator
import com.tiagohs.hqr.helpers.tools.BitmapCreator
import jp.wasabeef.picasso.transformations.BlurTransformation
import java.lang.Exception

class ImageUtils {

    companion object {

        fun load(imageView: ImageView, uri: Uri?, callback: Callback, skipCache: Boolean = false) {

            val loader = Picasso.get()
            var request: RequestCreator? = null

            if (uri != null)
                request = loader.load(uri)

            if (skipCache)
                request?.memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)

            request?.into(imageView, callback)
        }

        fun load(imageView: ImageView, url: String?, callback: Callback, skipCache: Boolean = false) {

            val loader = Picasso.get()
            var request: RequestCreator? = null

            if (!url.isNullOrEmpty())
                request = loader.load(url)

            if (skipCache)
                request?.memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)

            request!!.into(imageView, callback)
        }

        fun load(imageView: ImageView, url: String?) {
            load(imageView, url, null, null, false)
        }

        fun loadWithRevealAnimation(context: Context, imageView: ImageView, url: String?, placeholderPath: Int?, errorPath: Int?, callback: Callback? = null) {

            val loader = Picasso.get()
            var request: RequestCreator? = null

            if (!url.isNullOrEmpty())
                request = loader.load(url)

            if (placeholderPath != null)
                request!!.placeholder(placeholderPath)

            if (errorPath != null)
                request!!.error(errorPath)

            request!!.into(imageView, object: Callback {
                override fun onSuccess() {
                    try {
                        if (!(context as Activity).isDestroyed) {
                            AnimationUtils.createShowCircularReveal(imageView)
                            callback?.onSuccess()
                        }
                    } catch(ex: Exception) {
                        imageView.visibility = View.VISIBLE
                        callback?.onError(ex)
                    }
                }

                override fun onError(e: Exception?) {
                }
            })
        }

        fun loadWithBlur(imageView: ImageView, url: Int?, placeholderPath: Int?, errorPath: Int?, isToFit: Boolean? = true) {

            val loader = Picasso.get()
            var request: RequestCreator? = null

            if (url != null)
                request = loader.load(url)

            if (placeholderPath != null)
                request!!.placeholder(placeholderPath)

            if (errorPath != null)
                request!!.error(errorPath)

            request!!.transform(BlurTransformation(imageView.context))

            if (isToFit!!) {
                request.fit()
            }

            request.into(imageView)
        }

        fun loadWithBlur(imageView: ImageView, url: String?, placeholderPath: Int?, errorPath: Int?, isToFit: Boolean? = true) {

            val loader = Picasso.get()
            var request: RequestCreator? = null

            if (!url.isNullOrEmpty())
                request = loader.load(url)

            if (placeholderPath != null)
                request!!.placeholder(placeholderPath)

            if (errorPath != null)
                request!!.error(errorPath)

            request!!.transform(BlurTransformation(imageView.context))

            if (isToFit!!) {
                request.fit()
            }

            request.into(imageView)
        }

        fun load(imageView: ImageView, url: String?, placeholderPath: Int?, errorPath: Int?, isToFit: Boolean) {

            val loader = Picasso.get()
            var request: RequestCreator? = null

            if (!url.isNullOrEmpty())
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

            if (!url.isNullOrEmpty())
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