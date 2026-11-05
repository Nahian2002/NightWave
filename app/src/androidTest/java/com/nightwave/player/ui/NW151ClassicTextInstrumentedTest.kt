package com.nightwave.player.ui

import android.content.res.Configuration
import android.util.TypedValue
import android.view.View
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

/** Isolated Views, no Activity, personal preferences or system-setting writes. */
class NW151ClassicTextInstrumentedTest {
    @Test fun classicBrandMeasuresAtBoundedSpAcrossWidthsAndFontScales() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            for (scale in listOf(.8f,1f,1.3f,2f)) for (widthDp in listOf(120,240,320)) {
                val base=instrumentation.targetContext
                val config=Configuration(base.resources.configuration).apply { fontScale=scale }
                val context=base.createConfigurationContext(config)
                val view=NightWaveClassicTitleView(context).apply { text="NightWave · গান";maxLines=1 }
                val width=(widthDp*context.resources.displayMetrics.density).toInt()
                view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED))
                val minimum=TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,16f,context.resources.displayMetrics)
                val maximum=TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,23f,context.resources.displayMetrics)
                assertTrue(view.textSize>=minimum-.1f && view.textSize<=maximum+.1f);assertTrue(view.measuredHeight>0)
                assertEquals(width,view.measuredWidth)
            }
        }
    }
}
