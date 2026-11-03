package com.medusko.lagatron.model

import android.content.Context
import android.graphics.Typeface
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.res.ResourcesCompat
import com.medusko.lagatron.R

enum class AppFont(val title: String) {
    POPPINS("Poppins"),
    ROBOTO("Roboto"),
    MONOSPACE("Monospace"),
    CONDENSED("Condensed"),
    SERIF("Serif");

    fun getComposeFont(): FontFamily {
        return when (this) {
            POPPINS -> try {
                FontFamily(
                    Font(R.font.poppins_regular, FontWeight.Normal),
                    Font(R.font.poppins_bold, FontWeight.Bold)
                )
            } catch (_: Exception) {
                FontFamily.SansSerif
            }
            ROBOTO -> FontFamily.SansSerif
            MONOSPACE -> FontFamily.Monospace
            CONDENSED -> FontFamily.Default
            SERIF -> FontFamily.Serif
        }
    }

    fun getTypeface(context: Context): Typeface {
        return when (this) {
            POPPINS -> try {
                ResourcesCompat.getFont(context, R.font.poppins_regular) ?: Typeface.DEFAULT
            } catch (_: Exception) {
                Typeface.DEFAULT
            }
            ROBOTO -> Typeface.SANS_SERIF
            MONOSPACE -> Typeface.MONOSPACE
            CONDENSED -> Typeface.create("sans-serif-condensed", Typeface.NORMAL)
            SERIF -> Typeface.SERIF
        }
    }
}
