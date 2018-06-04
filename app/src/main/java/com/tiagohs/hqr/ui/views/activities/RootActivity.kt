package com.tiagohs.hqr.ui.views.activities

import android.os.Bundle
import com.tiagohs.hqr.R
import com.tiagohs.hqr.ui.views.config.BaseActivity
import com.tiagohs.hqr.ui.views.fragments.HomeFragment
import kotlinx.android.synthetic.main.activity_root.*

class RootActivity : BaseActivity() {
    override fun onGetMenuLayoutId(): Int {
        return 0
    }

    override fun onGetLayoutViewId(): Int {
        return R.layout.activity_root;
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onSetupBottomNavigation()
        supportActionBar!!.setDisplayHomeAsUpEnabled(false)
    }

    private fun onSetupBottomNavigation() {
        rootBottomNavigation!!.setOnNavigationItemSelectedListener({ item ->
            when (item.itemId) {
                R.id.actionHome -> {
                    startFragment(R.id.contentFragment, HomeFragment.newFragment())
                    true
                }
                R.id.actionProfile -> {

                    true
                }
                R.id.actionFavorite ->

                    true
            }

            false
        })

        rootBottomNavigation!!.setSelectedItemId(R.id.actionHome)
    }

}
