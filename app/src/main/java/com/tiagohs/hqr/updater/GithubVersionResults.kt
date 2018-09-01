package com.tiagohs.hqr.updater

import com.tiagohs.hqr.models.GithubRelease

sealed class GithubVersionResults {

    class NewUpdate(val release: GithubRelease): GithubVersionResults()
    class NoNewUpdate: GithubVersionResults()
}