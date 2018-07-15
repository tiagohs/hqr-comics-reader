package com.tiagohs.hqr.ui.views.activities

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.support.design.internal.BottomNavigationItemView
import android.support.design.internal.BottomNavigationMenuView
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import com.tiagohs.hqr.R
import com.tiagohs.hqr.download.DownloadManager
import com.tiagohs.hqr.helpers.utils.PermissionUtils
import com.tiagohs.hqr.helpers.utils.PermissionsCallback
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.ui.views.config.BaseActivity
import com.tiagohs.hqr.ui.views.fragments.DownloadManagerFragment
import com.tiagohs.hqr.ui.views.fragments.HomeFragment
import com.tiagohs.hqr.ui.views.fragments.LibrarieFragment
import com.tiagohs.hqr.ui.views.fragments.RecentFragment
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_root.*
import kotlinx.android.synthetic.main.notification_download_badge.view.*
import timber.log.Timber
import javax.inject.Inject


class RootActivity: BaseActivity(), PermissionsCallback {

    companion object {
        // Shortcut actions
        const val SHORTCUT_RECENTLY_UPDATED = "com.tiagohs.hqr.SHOW_RECENTLY_UPDATED"
        const val SHORTCUT_RECENTLY_READ = "com.tiagohs.hqr.SHOW_RECENTLY_READ"
        const val SHORTCUT_DOWNLOADS = "com.tiagohs.hqr.SHOW_DOWNLOADS"
        const val SHORTCUT_COMIC = "com.tiagohs.hqr.SHOW_MANGA"
    }

    val permissions: PermissionUtils = PermissionUtils(this)

    @Inject
    lateinit var downloadManager: DownloadManager

    var itemView: BottomNavigationItemView? = null

    var currentFragmentId: Int = -1

    override fun onGetMenuLayoutId(): Int {
        return 0
    }

    override fun onGetLayoutViewId(): Int {
        return R.layout.activity_root;
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        getApplicationComponent()?.inject(this)

        onSetupBottomNavigation()
        supportActionBar!!.setDisplayHomeAsUpEnabled(false)

        permissions.onCheckAndRequestPermissions(listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), this)

        onInit()
    }

    private fun onInit() {
        onObserveDownloadQueue()
    }

    override fun onErrorAction() {
        onInit()

        snack?.dismiss()
    }

    private fun onObserveDownloadQueue() {
        downloadManager.queue.getUpdatedStatus()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ configreDownloadBadge() },
                        { error ->
                            Timber.e(error)
                            onError(error, 0)
                        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        this.permissions.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    private fun onSetupBottomNavigation() {
        rootBottomNavigation!!.setOnNavigationItemSelectedListener({ item -> setItemSelected(item.itemId) })
        rootBottomNavigation!!.setSelectedItemId(R.id.actionHome)
    }

    private fun configreDownloadBadge() {

        if (!downloadManager.queue.isEmpty() && currentFragmentId != R.id.actionDownloads) {
            if (itemView == null) {
                val bottomNavigationMenuView = rootBottomNavigation.getChildAt(0) as BottomNavigationMenuView
                val v = bottomNavigationMenuView.getChildAt(3)
                itemView = v as BottomNavigationItemView

                val badge = LayoutInflater.from(this)
                        .inflate(R.layout.notification_download_badge, bottomNavigationMenuView, false)

                itemView?.addView(badge)
            }

            itemView?.downloadQueueCount?.visibility = View.VISIBLE
            itemView?.downloadQueueCount?.text = downloadManager.queue.size.toString()
        } else {
            itemView?.downloadQueueCount?.visibility = View.GONE
        }
    }

    override fun onNewIntent(intent: Intent) {
        if (!handleIntentReceiver(intent)) {
            super.onNewIntent(intent)
        }
    }

    private fun handleIntentReceiver(intent: Intent): Boolean {

        when (intent.action) {
            SHORTCUT_COMIC -> { openComic(intent.getParcelableExtra(SHORTCUT_COMIC))}
            SHORTCUT_DOWNLOADS -> rootBottomNavigation!!.setSelectedItemId(R.id.actionDownloads)
            SHORTCUT_RECENTLY_READ -> rootBottomNavigation!!.setSelectedItemId(R.id.actionRecent)
            else -> return false
        }

        return true

    }

    private fun openComic(comic: ComicViewModel) {
        startActivity(ComicDetailsActivity.newIntent(this, comic.pathLink!!))
    }

    private fun setItemSelected(itemId: Int): Boolean {
        currentFragmentId = itemId

        configreDownloadBadge()

        onChangeFragment(R.id.contentFragment, "tag:${itemId}", itemId)

        return true
    }

    fun onChangeFragment(container: Int, tag: String, itemId: Int): Fragment {
        val fm = supportFragmentManager
        val fragmentTransaction = fm.beginTransaction()

        val curFrag = fm.getPrimaryNavigationFragment()
        if (curFrag != null) {
            fragmentTransaction.detach(curFrag)
        }

        var fragment = fm.findFragmentByTag(tag)
        if (fragment == null) {
            fragment = getFragment(itemId)
            fragmentTransaction.add(container, fragment, tag)
        } else {
            fragmentTransaction.attach(fragment)
        }

        fragmentTransaction.setPrimaryNavigationFragment(fragment)
        fragmentTransaction.setReorderingAllowed(true)
        fragmentTransaction.commitNowAllowingStateLoss()

        return fragment
    }

    private fun getFragment(itemId: Int): Fragment {
        when (itemId) {
            R.id.actionHome -> return HomeFragment.newFragment()
            R.id.actionLibrarie -> return LibrarieFragment.newFragment()
            R.id.actionRecent -> return RecentFragment.newFragment()
            R.id.actionDownloads -> return DownloadManagerFragment.newFragment()
            else -> return HomeFragment.newFragment()
        }
    }

    override fun onPermissionsGranted() {

    }

    override fun onPermissionsDenied() {

    }

    override fun onNeverAskAgain(requestCode: Int) {

    }


}
