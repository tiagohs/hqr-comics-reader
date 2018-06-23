package com.tiagohs.hqr.ui.views.activities

import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import com.nononsenseapps.filepicker.AbstractFilePickerFragment
import com.nononsenseapps.filepicker.FilePickerActivity
import com.nononsenseapps.filepicker.FilePickerFragment
import com.nononsenseapps.filepicker.LogicHandler
import com.tiagohs.hqr.R
import java.io.File

class CustomLayoutPickerActivity : FilePickerActivity() {

    override fun getFragment(startPath: String?, mode: Int, allowMultiple: Boolean, allowCreateDir: Boolean, allowExistingFile: Boolean, singleClick: Boolean): AbstractFilePickerFragment<File> {
        val fragment = CustomLayoutFilePickerFragment()
        fragment.setArgs(startPath, mode, allowMultiple, allowCreateDir, allowExistingFile, singleClick)
        return fragment
    }
}

class CustomLayoutFilePickerFragment : FilePickerFragment() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            LogicHandler.VIEWTYPE_DIR -> {
                val inflater = layoutInflater
                val view = inflater.inflate(R.layout.item_dir, parent)
                return DirViewHolder(view)
            }
            else -> return super.onCreateViewHolder(parent, viewType)
        }
    }
}