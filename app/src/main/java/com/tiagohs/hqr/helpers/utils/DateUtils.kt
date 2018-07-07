package com.tiagohs.hqr.helpers.utils

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private val c = Calendar.getInstance()

    fun getYearByDate(dateString: String): Int {
        var date: Date? = null

        try {
            date = formatter.parse(dateString)
        } catch (e: ParseException) {
            return 0
        }

        val calendar = Calendar.getInstance()
        calendar.time = date
        return calendar.get(Calendar.YEAR)
    }

    fun getDateDayWeek(dayWeek: Int): String {
        c.set(Calendar.DAY_OF_WEEK, dayWeek)
        return formatter.format(c.time)
    }

    fun getDateToday(): String {
        val c = Calendar.getInstance()
        return formatter.format(c.time)
    }

    fun getCurrentYear(): String {
        val c = Calendar.getInstance()
        return c.get(Calendar.YEAR).toString()
    }

    fun getDateBefore(numDays: Int): String {
        val c = Calendar.getInstance()
        c.add(Calendar.DATE, -numDays)
        return formatter.format(c.time)
    }

    fun getDateAfter(numDays: Int): String {
        val c = Calendar.getInstance()
        c.add(Calendar.DATE, numDays)
        return formatter.format(c.time)
    }

    fun formateDate(dateString: String): String {
        var dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        var date: Date? = null
        try {
            date = dateFormat.parse(dateString)
        } catch (e: ParseException) {
            return dateString
        }

        dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)

        return dateFormat.format(date)
    }

    fun formateDate(format: String, dateString: String): String {
        var dateFormat = SimpleDateFormat(format, Locale.US)
        var date: Date? = null
        try {
            date = dateFormat.parse(dateString)
        } catch (e: ParseException) {
            return dateString
        }

        dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)

        return dateFormat.format(date)
    }

    fun formateStringToCalendar(dateString: String): Calendar {
        val cal = Calendar.getInstance()

        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            cal.time = sdf.parse(dateString)
        } catch (e: ParseException) {
            return Calendar.getInstance()
        }

        return cal
    }

}