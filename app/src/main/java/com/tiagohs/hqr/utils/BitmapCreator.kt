package com.tiagohs.hqr.utils

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.AsyncTask
import android.widget.ImageView
import java.lang.ref.WeakReference

class BitmapCreator(var mContext: Context?) {

    private var mBitmap: Bitmap? = null

    fun loadBitmap(resId: Int, imageView: ImageView?, placeholder: Bitmap) {
        if (cancelPotentialWork(resId, imageView!!)) {
            val task = BitmapWorkerTask(imageView)
            val asyncDrawable = AsyncDrawable(mContext!!.resources, placeholder, task)
            imageView.setImageDrawable(asyncDrawable)
            task.execute(resId)
        }
    }

    fun setBitmap(mBitmap: Bitmap) {
        this.mBitmap = mBitmap
    }

    private fun cancelPotentialWork(data: Int, imageView: ImageView): Boolean {
        val bitmapWorkerTask = getBitmapWorkerTask(imageView)

        if (bitmapWorkerTask != null) {
            val bitmapData = bitmapWorkerTask.data
            // If bitmapData is not yet set or it differs from the new data
            if (bitmapData == 0 || bitmapData != data) {
                // Cancel previous task
                bitmapWorkerTask.cancel(true)
            } else {
                // The same work is already in progress
                return false
            }
        }
        // No task associated with the ImageView, or an existing task was cancelled
        return true
    }

    private fun getBitmapWorkerTask(imageView: ImageView?): BitmapWorkerTask? {
        if (imageView != null) {
            val drawable = imageView.drawable
            if (drawable is AsyncDrawable) {
                return drawable.bitmapWorkerTask
            }
        }
        return null
    }

    private fun load(res: Resources, resId: Int?,
                     reqWidth: Int, reqHeight: Int): Bitmap {

        // First decode with inJustDecodeBounds=true to check dimensions
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeResource(res, resId!!, options)

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false

        return BitmapFactory.decodeResource(res, resId, options)
    }

    private fun calculateInSampleSize(
            options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        // Raw height and width of image
        //        final int height = options.outHeight;
        //        final int width = options.outWidth;
        val displayMetrics = mContext!!.resources.displayMetrics
        val height = displayMetrics.heightPixels / displayMetrics.density
        val width = displayMetrics.widthPixels / displayMetrics.density

        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {

            val halfHeight = height / 2
            val halfWidth = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    private inner class BitmapWorkerTask(imageView: ImageView) : AsyncTask<Int, Void, Bitmap>() {


        var imageViewReference: WeakReference<ImageView>?
        var data: Int? = 0

        init {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = WeakReference(imageView)
        }

        override fun doInBackground(vararg params: Int?): Bitmap {
            data = params[0]
            return load(mContext!!.resources, data, 100, 100)
        }

        // Once complete, see if ImageView is still around and set bitmap.
        override fun onPostExecute(bitmap: Bitmap?) {
            var bitmap = bitmap
            if (isCancelled) {
                bitmap = null
            }

            if (imageViewReference != null && bitmap != null) {
                val imageView = imageViewReference!!.get()
                val bitmapWorkerTask = getBitmapWorkerTask(imageView)
                setBitmap(bitmap)
                if (this === bitmapWorkerTask && imageView != null) {
                    imageView.setImageBitmap(bitmap)
                }
            }

        }
    }

    private class AsyncDrawable(res: Resources, bitmap: Bitmap,
                                bitmapWorkerTask: BitmapWorkerTask) : BitmapDrawable(res, bitmap) {
        private val bitmapWorkerTaskReference: WeakReference<BitmapWorkerTask>

        val bitmapWorkerTask: BitmapWorkerTask
            get() = bitmapWorkerTaskReference.get()!!

        init {
            bitmapWorkerTaskReference = WeakReference(bitmapWorkerTask)
        }
    }
}