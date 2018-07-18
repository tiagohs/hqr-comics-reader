package com.tiagohs.hqr.helpers.utils

import android.content.Context
import android.support.v4.content.ContextCompat
import android.widget.ImageView
import com.amulyakhare.textdrawable.TextDrawable
import com.amulyakhare.textdrawable.util.ColorGenerator
import com.tiagohs.hqr.R
import com.tiagohs.hqr.models.view_models.CANCELED
import com.tiagohs.hqr.models.view_models.COMPLETED
import com.tiagohs.hqr.models.view_models.ONGOING
import com.tiagohs.hqr.models.view_models.UNKNOWN

class ScreenUtils {

    companion object {

        fun calculateNoOfColumns(context: Context?, itemSizePixels: Float, defaultNumber: Int = 3): Int {
            try {
                if (context != null) {
                    val itemSizeDp = (itemSizePixels / context.resources?.getDisplayMetrics()!!.density).toInt()
                    val displayMetrics = context.getResources()!!.getDisplayMetrics()
                    val dpWidth = displayMetrics.widthPixels / displayMetrics.density

                    return (dpWidth / itemSizeDp).toInt()
                } else {
                    return defaultNumber
                }
            } catch (ex: Exception) {
                return defaultNumber
            }
        }

        fun generateMaterialColorBackground(context: Context?, text: String?, imageView: ImageView) : TextDrawable {
            val generator = ColorGenerator.MATERIAL

            if (!text.isNullOrEmpty()) {
                return TextDrawable.builder()
                        .beginConfig()
                        .endConfig()
                        .buildRound(text?.elementAt(0).toString().toUpperCase(), generator.getRandomColor());
            } else {
                return TextDrawable.builder()
                        .beginConfig()
                        .endConfig()
                        .buildRound("A", generator.getRandomColor());
            }
        }

        fun generateComicStatusBackground(context: Context, status: String?): Int {

            if (status != null) {
                when (status) {
                    COMPLETED -> return ContextCompat.getColor(context, R.color.green)
                    ONGOING -> return ContextCompat.getColor(context, R.color.blue)
                    CANCELED -> return ContextCompat.getColor(context, R.color.red)
                    UNKNOWN -> return ContextCompat.getColor(context, R.color.gray)
                }
            }

            return ContextCompat.getColor(context, R.color.gray)
        }

        fun getComicStatusText(context: Context, status: String?): String? {

            if (status != null) {
                when (status) {
                    COMPLETED -> return context.getString(R.string.completed_status)
                    ONGOING -> return context.getString(R.string.ongoing_status)
                    CANCELED -> return context.getString(R.string.canceled_status)
                    UNKNOWN -> return context.getString(R.string.unknown_status)
                }
            }

            return context.getString(R.string.unknown_status)
        }

        fun getStatusConstant(status: String?): String? {

            if (status != null) {
                if (status.toLowerCase().contains("concluído") or status.contains("completed") or status.contains(COMPLETED)) {
                    return COMPLETED
                } else if (status.toLowerCase().contains("em andamento") or status.contains("ongoing") or status.contains(ONGOING)) {
                    return ONGOING
                } else if (status.toLowerCase().contains("cancelado") or status.contains("canceled") or status.contains(CANCELED)) {
                    return CANCELED
                }
            }

            return UNKNOWN
        }

    }


}