package com.tiagohs.hqr.ui.views.activities

import android.os.Bundle
import com.tiagohs.hqr.R
import com.tiagohs.hqr.ui.adapters.ComicDetailsPagerAdapter
import com.tiagohs.hqr.ui.views.config.BaseActivity
import kotlinx.android.synthetic.main.activity_comic_details.*

class ComicDetailsActivity: BaseActivity() {

    override fun onGetMenuLayoutId(): Int {
        return R.menu.menu_comic_details
    }

    override fun onGetLayoutViewId(): Int {
        return R.layout.activity_comic_details
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        comicsDetailsViewpager.adapter = ComicDetailsPagerAdapter(supportFragmentManager, mutableListOf("Resume", "Chapters"))
        tabLayout.setupWithViewPager(comicsDetailsViewpager)
    }
}