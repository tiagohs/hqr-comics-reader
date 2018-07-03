package com.tiagohs.hqr.ui.views.activities

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.support.design.internal.BottomNavigationItemView
import android.support.design.internal.BottomNavigationMenuView
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import com.tiagohs.hqr.R
import com.tiagohs.hqr.helpers.utils.PermissionUtils
import com.tiagohs.hqr.helpers.utils.PermissionsCallback
import com.tiagohs.hqr.ui.views.config.BaseActivity
import com.tiagohs.hqr.ui.views.fragments.DownloadManagerFragment
import com.tiagohs.hqr.ui.views.fragments.HomeFragment
import com.tiagohs.hqr.ui.views.fragments.LibrarieFragment
import com.tiagohs.hqr.ui.views.fragments.RecentFragment
import kotlinx.android.synthetic.main.activity_root.*



class RootActivity: BaseActivity(), PermissionsCallback {

    companion object {
        // Shortcut actions
        const val SHORTCUT_RECENTLY_UPDATED = "com.tiagohs.hqr.SHOW_RECENTLY_UPDATED"
        const val SHORTCUT_RECENTLY_READ = "com.tiagohs.hqr.SHOW_RECENTLY_READ"
        const val SHORTCUT_DOWNLOADS = "com.tiagohs.hqr.SHOW_DOWNLOADS"
        const val SHORTCUT_COMIC = "com.tiagohs.hqr.SHOW_MANGA"
    }

    val permissions: PermissionUtils = PermissionUtils(this)

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

        permissions.onCheckAndRequestPermissions(listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), this)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        this.permissions.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    private fun onSetupBottomNavigation() {
        rootBottomNavigation!!.setOnNavigationItemSelectedListener({ item -> setItemSelected(item.itemId) })
        rootBottomNavigation!!.setSelectedItemId(R.id.actionHome)

        configreDownloadBadge()
    }

    private fun configreDownloadBadge() {
        val bottomNavigationMenuView = rootBottomNavigation.getChildAt(0) as BottomNavigationMenuView
        val v = bottomNavigationMenuView.getChildAt(2)
        val itemView = v as BottomNavigationItemView

        val badge = LayoutInflater.from(this)
                .inflate(R.layout.notification_download_badge, bottomNavigationMenuView, false)

        itemView.addView(badge)
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

    override fun onPermissionsGranted() {

    }

    override fun onPermissionsDenied() {

    }

    override fun onNeverAskAgain(requestCode: Int) {

    }


}
