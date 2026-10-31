package com.tiagohs.hqr.app

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IosChallengeCookiesTest {
    private val cookies = IosChallengeCookies(MapSettings())

    @Test
    fun RF106_a_clearance_of_the_site_serves_its_subdomains() {
        cookies.save("weebcentral.com", "cf_clearance=abc; other=1", "Safari")
        assertEquals("cf_clearance=abc; other=1", cookies.cookies("https://api.weebcentral.com/chapters"))
        assertEquals("cf_clearance=abc; other=1", cookies.cookies("https://weebcentral.com/"))
        assertEquals("Safari", cookies.userAgent)
    }

    @Test
    fun RF106_other_sites_and_cookies_without_clearance_get_nothing() {
        cookies.save("weebcentral.com", "cf_clearance=abc", "Safari")
        cookies.save("xoxo.com", "session=1", "Safari")
        assertNull(cookies.cookies("https://example.com/"))
        assertNull(cookies.cookies("https://xoxo.com/"))
        assertNull(cookies.cookies("https://com/"))
    }
}
