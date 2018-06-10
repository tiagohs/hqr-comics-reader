package com.tiagohs.hqr.models.viewModels

import com.tiagohs.hqr.models.sources.ComicsItem

class ComicsListModel(
        var comics: List<ComicsItem>,
        var hasPagesSupport: Boolean
) {

}