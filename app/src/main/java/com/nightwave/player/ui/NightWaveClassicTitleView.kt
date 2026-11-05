package com.nightwave.player.ui

import android.content.Context
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.TextView

/** Platform-only API23 backport for the retained classic brand label; no theme or data migration. */
class NightWaveClassicTitleView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = android.R.attr.textViewStyle
) : TextView(context, attrs, defStyleAttr) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec) - compoundPaddingLeft - compoundPaddingRight
        if (width > 0 && MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED) {
            val content = transformationMethod?.getTransformation(text, this) ?: text
            val height = if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) Int.MAX_VALUE
                else MeasureSpec.getSize(heightMeasureSpec) - compoundPaddingTop - compoundPaddingBottom
            val chosen = ClassicTextSizePolicy.choose(16, 23) { sp ->
                val probe = TextPaint(paint).apply { textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp.toFloat(), resources.displayMetrics) }
                val layout = StaticLayout.Builder.obtain(content, 0, content.length, probe, width)
                    .setMaxLines(1).setIncludePad(includeFontPadding).build()
                layout.height <= height && layout.getLineEnd(layout.lineCount - 1) == content.length &&
                    layout.getLineWidth(0) <= width
            }
            val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, chosen.toFloat(), resources.displayMetrics)
            if (kotlin.math.abs(textSize - px) > .1f) super.setTextSize(TypedValue.COMPLEX_UNIT_PX, px)
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}
