package com.tiagohs.hqr.utils

import android.content.Context
import com.amulyakhare.textdrawable.TextDrawable
import com.amulyakhare.textdrawable.util.ColorGenerator

class ScreenUtils {

    companion object {
        fun calculateNoOfColumns(context: Context?, itemSize: Int): Int {
            val displayMetrics = context?.getResources()!!.getDisplayMetrics()
            val dpWidth = displayMetrics.widthPixels / displayMetrics.density
            return (dpWidth / itemSize).toInt()
        }

        fun generateMaterialColorBackground(context: Context?) : TextDrawable {
            val generator = ColorGenerator.MATERIAL

            return TextDrawable.builder()
                    .beginConfig()
                    .endConfig()
                    .buildRoundRect("", generator.getRandomColor(), 20);
        }
    }


}