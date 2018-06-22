package com.tiagohs.hqr.helpers.utils

import android.content.Context
import android.content.res.Resources
import com.tiagohs.hqr.R
import com.tiagohs.hqr.models.sources.LocaleDTO
import java.util.*

class LocaleUtils {
    companion object {

        fun getDisplayLanguageAndCountryName(context: Context, language: String): String {
            when (language) {
                "PT-BR" -> return context.getString(R.string.portuguese)
                "EN" -> return context.getString(R.string.english)

                else -> return context.getString(R.string.english)
            }
        }

        fun getLocaleAtual(): Locale {
            return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
                Resources.getSystem().configuration.locales.get(0)
            else
                Resources.getSystem().configuration.locale
        }

        fun getLocaleLanguageISO(): String {
            return getLocaleAtual().language
        }

        fun getLocaleLanguageISO(locale: Locale): String {
            return getLocaleAtual().language
        }

        fun getLocaleLanguageName(languageIso: String): String {
            return getLocaleAtual().getDisplayLanguage(Locale(languageIso))
        }

        fun getLocaleLanguageName(): String {
            return getLocaleAtual().displayLanguage
        }

        fun getLocaleCountryISO(): String {
            return getLocaleAtual().country
        }

        fun getLocaleCountryISO(locale: Locale): String {
            return locale.country
        }

        fun getLocaleCountryName(): String {
            return getLocaleAtual().displayCountry
        }

        fun getLocaleCountryName(languageIso: String): String {
            return getLocaleAtual().getDisplayCountry(Locale(languageIso))
        }

        fun getLocaleLanguageAndCountry(): String {
            return getLocaleLanguageISO() + "-" + getLocaleCountryISO()
        }

        fun getLocaleLanguageAndCountry(locale: Locale): String {
            return getLocaleLanguageISO(locale) + "-" + getLocaleCountryISO(locale)
        }

        fun getAllCountrys(): List<String> {
            val locales = Locale.getAvailableLocales()
            val countries = ArrayList<String>()
            for (locale in locales) {
                val country = locale.displayCountry
                if (country.trim { it <= ' ' }.length > 0 && !countries.contains(country)) {
                    countries.add(country)
                }
            }

            Collections.sort(countries)

            return countries
        }

        fun getAllCountrysDTO(): List<LocaleDTO> {
            val locales = Locale.getAvailableLocales()
            val countries: ArrayList<LocaleDTO> = ArrayList()

            var localeDTO: LocaleDTO
            for (locale in locales) {
                try {
                    val country = locale.displayCountry
                    val iso = locale.isO3Country
                    val code = locale.country
                    val name = locale.displayCountry
                    if (country.trim { it <= ' ' }.length > 0 && "" != iso && "" != code && "" != name) {
                        localeDTO = LocaleDTO(name, locale.displayLanguage, iso, locale.isO3Language, locale)
                        if (!countries.contains(localeDTO))
                            countries.add(localeDTO)
                    }
                } catch (e: MissingResourceException) {

                }

            }

            Collections.sort(countries)

            return countries
        }
    }
}