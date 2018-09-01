package com.tiagohs.hqr.models

import com.google.gson.annotations.SerializedName

class GithubRelease(
        @SerializedName("tag_name") val version: String,
        @SerializedName("body") val changeLog: String,
        @SerializedName("assets") val assets: List<Assets>
) {

    var downloadLink = assets[0].downloadLink

    inner class Assets(@SerializedName("browser_download_url") val downloadLink: String)
}