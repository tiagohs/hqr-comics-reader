package com.tiagohs.hqr.models.view_models

import com.tiagohs.hqr.models.sources.Page

class PageItem(
        val page: Page?,
        val pageType: Int
) {

    companion object {
        const val COMIC_PAGE = 0
        const val AD_PAGE = 1

    }

}