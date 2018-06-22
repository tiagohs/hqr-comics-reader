package com.tiagohs.hqr.models.sources

import java.util.*

class LocaleDTO(
        val displayCountryName: String,
        val displayCountryLanguage: String,
        val isoCountry: String,
        val isoLanguage: String,
        val locale: Locale
): Comparable<LocaleDTO> {

    override fun compareTo(o: LocaleDTO): Int {
        return displayCountryName.compareTo(o.displayCountryName)
    }

}