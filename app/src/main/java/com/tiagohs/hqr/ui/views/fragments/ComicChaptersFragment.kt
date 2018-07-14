package com.tiagohs.hqr.ui.views.fragments

import android.os.Bundle
import android.support.v7.view.ActionMode
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import com.tiagohs.hqr.R
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.ui.adapters.chapters.ChapterHolder
import com.tiagohs.hqr.ui.adapters.chapters.ChapterItem
import com.tiagohs.hqr.ui.adapters.chapters.ChaptersListAdapter
import com.tiagohs.hqr.ui.callbacks.IChapterItemCallback
import com.tiagohs.hqr.ui.contracts.ComicChaptersContract
import com.tiagohs.hqr.ui.views.activities.ComicDetailsActivity
import com.tiagohs.hqr.ui.views.activities.ReaderActivity
import com.tiagohs.hqr.ui.views.config.BaseFragment
import eu.davidea.flexibleadapter.SelectableAdapter
import kotlinx.android.synthetic.main.fragment_comic_chapters.*
import javax.inject.Inject

private const val COMIC = "comic_link"

class ComicChaptersFragment:
        BaseFragment(),
        IChapterItemCallback,
        ComicChaptersContract.IComicChaptersView,
        ActionMode.Callback {

    companion object {
        fun newFragment(comic: ComicViewModel): ComicChaptersFragment {
            val bundle = Bundle()
            bundle.putParcelable(COMIC, comic)

            val fragment = ComicChaptersFragment()
            fragment.arguments = bundle

            return fragment
        }
    }

    @Inject
    lateinit var presenter: ComicChaptersContract.IComicChaptersPresenter

    private var actionMode: ActionMode? = null
    private val selectedItems = mutableSetOf<ChapterItem>()

    private var adapter: ChaptersListAdapter? = null
    private var deletingDialog: MaterialDialog? = null
    private var pageLoaded = false
    private var comicViewModel: ComicViewModel? = null

    private var actionModeMenu: Menu? = null

    override fun getViewID(): Int {
        return R.layout.fragment_comic_chapters
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        comicViewModel = arguments?.getParcelable(COMIC)
    }


    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)

        menu!!.clear()
        inflater!!.inflate(R.menu.menu_chapters, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        when (item!!.itemId) {
            R.id.actionDownload -> createActionMode()
            else -> return false
        }

        return true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getApplicationComponent()!!.inject(this)
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (isVisibleToUser && !pageLoaded) {
            onViewFocus()
            pageLoaded = true
        }
    }

    private fun onViewFocus() {
        presenter.onBindView(this)
        presenter.onCreate(comicViewModel)

        adapter = ChaptersListAdapter(context, this)

        chaptersList.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        chaptersList.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        chaptersList.adapter = adapter
    }

    override fun onDestroyView() {
        adapter = null
        actionMode = null

        presenter.onUnbindView()

        super.onDestroyView()
    }

    override fun onNextChapters(chapters: List<ChapterItem>) {
        val adapter = adapter ?: return
        adapter.updateDataSet(chapters)

        if (selectedItems.isNotEmpty()) {
            adapter.clearSelection()
            createActionMode()

            selectedItems.forEach { chapter ->
                val position = adapter.indexOf(chapter)

                if (position != -1 && !adapter.isSelected(position)) {
                    adapter.toggleSelection(position)
                }
            }

            actionMode?.invalidate()
        }

        chaptersProgress.visibility = View.GONE
        chaptersList.visibility = View.VISIBLE
    }

    override fun onChapterStatusChange(chapterItem: ChapterItem, status: Int) {
        getHolder(chapterItem)?.notifyStatus(status)
    }

    private fun getHolder(chapter: ChapterItem): ChapterHolder? {
        return chaptersList?.findViewHolderForItemId(chapter.chapter.id) as? ChapterHolder
    }

    override fun onDownloadSelect(chapter: ChapterItem) {
        presenter.downloadChapters(listOf(chapter))
    }

    override fun onItemClick(view: View?, position: Int): Boolean {
        val adapter = adapter ?: return false
        val item = adapter.getItem(position) ?: return false

        if (actionMode != null && adapter.mode == SelectableAdapter.Mode.MULTI) {
            toggleComic(position)
            return true
        } else {
            startActivity(ReaderActivity.newIntent(context, item.chapter.chapterPath!!, comicViewModel?.pathLink!!))
            return false
        }
    }

    override fun onItemLongClick(position: Int) {
        createActionMode()
        toggleComic(position)
    }

    private fun toggleComic(position: Int) {
        val adapter = adapter ?: return
        val item = adapter.getItem(position) ?: return

        adapter.toggleSelection(position)

        if (adapter.isSelected(position)) {
            selectedItems.add(item)
        } else {
            selectedItems.remove(item)
        }

        actionMode?.invalidate()

    }

    private fun createActionMode() {
        if (actionMode == null) {
            actionMode = (activity as? ComicDetailsActivity)?.startSupportActionMode(this)
        }
    }

    private fun destroyActionMode() {
        actionMode?.finish()
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu?): Boolean {
        mode.menuInflater.inflate(R.menu.menu_chapter_selection, menu)
        adapter?.mode = SelectableAdapter.Mode.MULTI
        mode.title = context?.getString(R.string.chapters_selected_title)

        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu?): Boolean {
        actionModeMenu = menu

        val count = adapter?.selectedItemCount ?: 0

        if (count == 0) {
            mode.title = context?.getString(R.string.chapters_selected_title)
        } else {
            mode.title = context?.getString(R.string.chapters_selected, count)
        }

        onFilterMenuOptions()

        return false
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        adapter?.mode = SelectableAdapter.Mode.SINGLE
        adapter?.clearSelection()
        selectedItems.clear()
        actionMode = null
        actionModeMenu = null
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem): Boolean {

        when(item.itemId) {
            R.id.actionDownloadSelected -> downloadChapters(getSelectedChapters())
            R.id.actionDelete -> showDeleteChaptersConfirmationDialog()
            R.id.actionSelectAll -> selectAllChapters()
            R.id.actionClearAll -> clearAll()
            else -> return false
        }

        return true
    }

    private fun onFilterMenuOptions() {
        actionModeMenu?.findItem(R.id.actionDelete)?.isVisible = getSelectedChapters().filter { it.isDownloaded }.isNotEmpty()
        actionModeMenu?.findItem(R.id.actionDownloadSelected)?.isVisible = getSelectedChapters().filter { !it.isDownloaded }.isNotEmpty()
        actionModeMenu?.findItem(R.id.actionSelectAll)?.isVisible = getSelectedChapters().size < adapter?.items?.size!!
        actionModeMenu?.findItem(R.id.actionClearAll)?.isVisible = getSelectedChapters().isNotEmpty()
    }

    private fun clearAll() {
        val adapter = adapter ?: return
        adapter.clearSelection()
        selectedItems.clear()
        actionMode?.invalidate()
    }

    private fun selectAllChapters() {
        val adapter = adapter ?: return
        adapter.selectAll()
        selectedItems.addAll(adapter.items)
        actionMode?.invalidate()
    }

    private fun showDeleteChaptersConfirmationDialog() {
        val count = adapter?.selectedItemCount ?: return

        MaterialDialog.Builder(context!!)
                    .content(resources.getQuantityString(R.plurals.confirm_delete_chapters, count))
                    .positiveText(android.R.string.yes)
                    .negativeText(android.R.string.no)
                    .onPositive { _, _ -> deleteChapters(getSelectedChapters()) }
                    .build()
                    .show()
    }

    private fun deleteChapters(items: List<ChapterItem>) {
        destroyActionMode()
        if (items.isEmpty()) return

        deletingDialog = MaterialDialog.Builder(context!!)
                                    .progress(true, 0)
                                    .content(R.string.deleting)
                                    .cancelable(false)
                                    .build()

        deletingDialog?.show()
        presenter.deleteChapters(items)
    }

    override fun onChapterDeleted() {
        dimissDeletingDialog()
        adapter?.notifyDataSetChanged()
    }

    override fun onChapterDeletedError() {
        dimissDeletingDialog()
    }

    private fun dimissDeletingDialog() {
        deletingDialog?.dismiss()
    }

    private fun getSelectedChapters(): List<ChapterItem> {
        val adapter = adapter ?: return emptyList()
        return adapter.selectedPositions.mapNotNull { adapter.getItem(it) }
    }

    private fun downloadChapters(selectedChapters: List<ChapterItem>) {
        destroyActionMode()
        presenter.downloadChapters(selectedChapters)
    }

}