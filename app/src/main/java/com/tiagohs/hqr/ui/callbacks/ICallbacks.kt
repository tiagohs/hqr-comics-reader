package com.tiagohs.hqr.ui.callbacks

import eu.davidea.flexibleadapter.FlexibleAdapter

interface ICallbacks: FlexibleAdapter.OnItemClickListener,
        FlexibleAdapter.OnItemLongClickListener,
        FlexibleAdapter.EndlessScrollListener {
}