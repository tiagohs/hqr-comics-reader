package com.tiagohs.hqr.ui.views.activities

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import com.tiagohs.hqr.R
import com.tiagohs.hqr.ui.views.config.BaseActivity
import com.tiagohs.hqr.ui.views.fragments.DownloadManagerFragment
import com.tiagohs.hqr.ui.views.fragments.HomeFragment
import com.tiagohs.hqr.ui.views.fragments.LibrarieFragment
import com.tiagohs.hqr.ui.views.fragments.RecentFragment
import kotlinx.android.synthetic.main.activity_root.*

class RootActivity : BaseActivity() {

    companion object {
        // Shortcut actions
        const val SHORTCUT_RECENTLY_UPDATED = "com.tiagohs.hqr.SHOW_RECENTLY_UPDATED"
        const val SHORTCUT_RECENTLY_READ = "com.tiagohs.hqr.SHOW_RECENTLY_READ"
        const val SHORTCUT_DOWNLOADS = "com.tiagohs.hqr.SHOW_DOWNLOADS"
        const val SHORTCUT_COMIC = "com.tiagohs.hqr.SHOW_MANGA"
    }

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
        rootBottomNavigation!!.setOnNavigationItemSelectedListener({ item -> setItemSelected(item.itemId) })
        rootBottomNavigation!!.setSelectedItemId(R.id.actionHome)
    }

    override fun onNewIntent(intent: Intent) {
        if (!handleIntentReceiver(intent)) {
            super.onNewIntent(intent)
        }
    }

    private fun handleIntentReceiver(intent: Intent): Boolean {

        when (intent.action) {
            SHORTCUT_COMIC -> {}
            SHORTCUT_DOWNLOADS -> setItemSelected(R.id.actionDownload)
            SHORTCUT_RECENTLY_READ -> setItemSelected(R.id.actionRecent)
            else -> return false
        }

        return true

    }

    private fun setItemSelected(itemId: Int): Boolean {
        when (itemId) {
            R.id.actionHome -> startFragment(R.id.contentFragment, HomeFragment.newFragment())
            R.id.actionLibrarie -> startFragment(R.id.contentFragment, LibrarieFragment.newFragment())
            R.id.actionRecent -> startFragment(R.id.contentFragment, RecentFragment.newFragment())
            R.id.actionDownloads -> startFragment(R.id.contentFragment, DownloadManagerFragment.newFragment())
            else -> return false
        }

        return true
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
