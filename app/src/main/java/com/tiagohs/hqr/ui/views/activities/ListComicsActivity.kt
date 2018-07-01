package com.tiagohs.hqr.ui.views.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.tiagohs.hqr.R
import com.tiagohs.hqr.models.view_models.ListComicsModel
import com.tiagohs.hqr.ui.views.config.BaseActivity
import com.tiagohs.hqr.ui.views.fragments.ListComicsFragment

private const val LIST_MODEL = "LIST_MODEL"

class ListComicsActivity: BaseActivity() {

    companion object {
        fun newIntent(context: Context?, model: ListComicsModel): Intent {
            val intent = Intent(context, ListComicsActivity::class.java)

            intent.putExtra(LIST_MODEL, model)

            return intent
        }
    }

    override fun onGetLayoutViewId(): Int = R.layout.activity_list_comics
    override fun onGetMenuLayoutId(): Int  = 0

    lateinit var listModel: ListComicsModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        listModel = intent.getParcelableExtra(LIST_MODEL)

        setScreenTitle(listModel.pageTitle)

        startFragment(R.id.contentFragment, ListComicsFragment.newFragment(listModel))
    }

}