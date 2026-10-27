package com.kms.kmspricescanner

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class GoldShineTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var linearGradient: LinearGradient? = null
    private var gradientMatrix: Matrix? = null
    private var paint: Paint? = null
    private var mViewWidth = 0
    private var translate = 0f
    private var animator: ValueAnimator? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (mViewWidth == 0) {
            mViewWidth = measuredWidth
            if (mViewWidth > 0) {
                paint = getPaint()

                // الألوان: ذهبي غامق -> ذهبي ساطع (اللمعة) -> ذهبي غامق
                val colors = intArrayOf(
                    Color.parseColor("#B8860B"), // Dark Gold
                    Color.parseColor("#FFF8DC"), // White Gold (لمعة ساطعة)
                    Color.parseColor("#B8860B")  // Dark Gold
                )

                linearGradient = LinearGradient(
                    0f, 0f, mViewWidth.toFloat(), 0f,
                    colors,
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
                paint?.shader = linearGradient
                gradientMatrix = Matrix()

                startAnimation()
            }
        }
    }

    private fun startAnimation() {
        // تحريك اللمعة من اليسار لليمين
        animator = ValueAnimator.ofFloat(0f, 1f)
        animator?.duration = 2500 // سرعة اللمعان (2.5 ثانية)
        animator?.repeatCount = ValueAnimator.INFINITE // تكرار لا نهائي
        animator?.addUpdateListener { animation ->
            val value = animation.animatedValue as Float
            translate = mViewWidth * 2 * value - mViewWidth
            gradientMatrix?.setTranslate(translate, 0f)
            linearGradient?.setLocalMatrix(gradientMatrix)
            invalidate()
        }
        animator?.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel() // إيقاف الأنيميشن عند إغلاق الشاشة لتوفير البطارية
    }
}