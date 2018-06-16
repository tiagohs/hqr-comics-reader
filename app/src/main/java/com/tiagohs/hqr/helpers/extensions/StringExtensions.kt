package com.tiagohs.hqr.helpers.extensions

fun String.chop(count: Int, replacement: String = "..."): String {
    return if (length > count)
        take(count - replacement.length) + replacement
    else
        this

}

fun String.truncateCenter(count: Int, replacement: String = "..."): String{
    if(length <= count)
        return this

    val pieceLength:Int = Math.floor((count - replacement.length).div(2.0)).toInt()

    return "${ take(pieceLength) }$replacement${ takeLast(pieceLength) }"
}