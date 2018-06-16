package com.tiagohs.hqr.helpers.utils

import android.content.Context
import android.support.v4.content.ContextCompat
import android.widget.ImageView
import com.amulyakhare.textdrawable.TextDrawable
import com.amulyakhare.textdrawable.util.ColorGenerator
import com.tiagohs.hqr.R
import com.tiagohs.hqr.models.sources.CANCELED
import com.tiagohs.hqr.models.sources.COMPLETED
import com.tiagohs.hqr.models.sources.ONGOING
import com.tiagohs.hqr.models.sources.UNKNOWN

class ScreenUtils {

    companion object {

        fun calculateNoOfColumns(context: Context?, itemSize: Int): Int {
            val displayMetrics = context?.getResources()!!.getDisplayMetrics()
            val dpWidth = displayMetrics.widthPixels / displayMetrics.density
            return (dpWidth / itemSize).toInt()
        }

        fun generateMaterialColorBackground(context: Context?, text: String, imageView: ImageView) : TextDrawable {
            val generator = ColorGenerator.MATERIAL

            return TextDrawable.builder()
                    .beginConfig()
                    .endConfig()
                    .buildRound(text.elementAt(0).toString(), generator.getRandomColor());
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
                if (status.contains("Concluído") or status.contains("Completed")) {
                    return COMPLETED
                } else if (status.contains("Em Andamento") or status.contains("Ongoing")) {
                    return ONGOING
                } else if (status.contains("Cancelado") or status.contains("Canceled")) {
                    return CANCELED
                }
            }

            return UNKNOWN
        }

    }


}