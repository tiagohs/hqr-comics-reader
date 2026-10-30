package com.tiagohs.hqr.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.tiagohs.hqr.ui.resources.Res
import com.tiagohs.hqr.ui.resources.bangers_regular
import com.tiagohs.hqr.ui.resources.roboto_flex
import org.jetbrains.compose.resources.Font

/** Roboto Flex for the interface; titles use its width 75 and weight 800 (design.md §3). */
@Composable
fun robotoFlex(): FontFamily =
    FontFamily(
        Font(Res.font.roboto_flex, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
        Font(Res.font.roboto_flex, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
        Font(Res.font.roboto_flex, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
        Font(Res.font.roboto_flex, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    )

/** The condensed heavy cut used only in titles: lettering without a new font. */
@Composable
fun condensed(): FontFamily =
    FontFamily(
        Font(
            Res.font.roboto_flex,
            FontWeight.ExtraBold,
            variationSettings = FontVariation.Settings(FontVariation.weight(800), FontVariation.width(75f)),
        ),
    )

/** Bangers: logo, long-loading onomatopoeia, empty states and chapter transition only. */
@Composable
fun bangers(): FontFamily = FontFamily(Font(Res.font.bangers_regular))

@Composable
fun hqrTypography(): Typography {
    val body = robotoFlex()
    val title = condensed()
    val base = Typography()

    fun TextStyle.body() = copy(fontFamily = body)

    fun TextStyle.title() = copy(fontFamily = title, fontWeight = FontWeight.ExtraBold)
    return Typography(
        displayLarge = base.displayLarge.title(),
        displayMedium = base.displayMedium.title(),
        displaySmall = base.displaySmall.title(),
        headlineLarge = base.headlineLarge.title().copy(fontSize = 30.sp, lineHeight = 36.sp),
        headlineMedium = base.headlineMedium.title().copy(fontSize = 26.sp, lineHeight = 30.sp),
        headlineSmall = base.headlineSmall.title().copy(fontSize = 22.sp, lineHeight = 26.sp),
        titleLarge = base.titleLarge.title().copy(fontSize = 20.sp),
        titleMedium = base.titleMedium.body().copy(fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.body().copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.body(),
        bodyMedium = base.bodyMedium.body(),
        bodySmall = base.bodySmall.body(),
        labelLarge = base.labelLarge.body().copy(fontWeight = FontWeight.SemiBold),
        labelMedium = base.labelMedium.body().copy(fontWeight = FontWeight.SemiBold),
        labelSmall = base.labelSmall.body().copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
    )
}
