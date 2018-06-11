package com.tiagohs.hqr.ui.views.activities

import android.os.Bundle
import android.support.v4.app.Fragment
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
                R.id.actionHome -> onSelectFragment("${item.itemId}:${R.id.actionHome}", HomeFragment.newFragment())
                R.id.actionFavorite -> {

                    true
                }
                R.id.actionRecent -> {

                    true
                }
                R.id.actionMore ->

                    true
                }

            false
        })

        rootBottomNavigation!!.setSelectedItemId(R.id.actionHome)
    }


    private fun onSelectFragment(tag: String, fragmentSelect: Fragment) {
        val fm = supportFragmentManager
        var fragment = fm.findFragmentByTag(tag)
        val fmTransaction = supportFragmentManager.beginTransaction()

        if (fragment == null) {
            fragment = fragmentSelect
            fmTransaction.add(R.id.contentFragment, fragment, tag)
        } else {
            val curFrag = supportFragmentManager.getPrimaryNavigationFragment()

            if (curFrag != null) {
                fmTransaction.detach(curFrag)
            }

            fmTransaction.attach(fragment)
        }

        fmTransaction.setPrimaryNavigationFragment(fragment)
                .setReorderingAllowed(true)
                .commitNowAllowingStateLoss()
    }


}
