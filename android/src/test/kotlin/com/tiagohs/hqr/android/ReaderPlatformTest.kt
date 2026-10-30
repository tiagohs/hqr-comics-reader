package com.tiagohs.hqr.android

import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import com.tiagohs.hqr.ui.reader.KeepScreenOn
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The reader's Android pieces on a real view tree (RF-46). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReaderPlatformTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun RF46_keepScreenOnHoldsTheScreenWhileReadingAndLetsGoAfter() {
        var view: View? = null
        var on by mutableStateOf(true)
        compose.setContent {
            view = LocalView.current
            KeepScreenOn(on)
        }
        compose.waitForIdle()
        assertTrue(view!!.keepScreenOn)
        on = false
        compose.waitForIdle()
        assertFalse(view!!.keepScreenOn)
    }
}
