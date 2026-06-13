package com.kms.kmspricescanner

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.FrameLayout
import java.util.Random

class ThemeEffectHelper(private val activity: Activity) {

    private val random = Random()

    fun showTapEffect(x: Float, y: Float, effectType: String) {
        val iconRes = when (effectType.lowercase().trim()) { // .trim() لإزالة المسافات الزائدة
            "snow" -> R.drawable.ic_effect_snow
            "ramadan" -> R.drawable.ic_effect_ramadan
            else -> return
        }

        // استخدام decorView لضمان الرسم فوق التطبيق بالكامل
        val rootView = activity.window.decorView as ViewGroup

        for (i in 0..6) {
            createParticle(rootView, x, y, iconRes)
        }
    }

    private fun createParticle(root: ViewGroup, startX: Float, startY: Float, iconRes: Int) {
        val particle = ImageView(activity)
        particle.setImageResource(iconRes)

        // ⚠️ هام جداً: جعل الصورة تظهر فوق كل العناصر الأخرى
        particle.elevation = 100f

        // حجم عشوائي
        val size = 40 + random.nextInt(50)
        val params = FrameLayout.LayoutParams(size, size)
        particle.layoutParams = params

        particle.x = startX
        particle.y = startY

        root.addView(particle)

        // حركة الانفجار
        val destX = startX + (random.nextInt(500) - 250)
        val destY = startY + (random.nextInt(500) - 250) + 400 // تسقط لأسفل
        val rotation = random.nextInt(360).toFloat()

        particle.animate()
            .translationX(destX)
            .translationY(destY)
            .rotation(rotation)
            .alpha(0f)
            .setDuration(1000 + random.nextInt(500).toLong())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    root.removeView(particle)
                }
            })
            .start()
    }
}