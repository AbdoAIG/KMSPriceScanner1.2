package com.kms.kmspricescanner

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.CycleInterpolator
import android.view.animation.LinearInterpolator

object LogoAnimator {

    fun applyAnimation(view: View, animType: String) {
        // 1. إيقاف أي حركة سابقة لتجنب التداخل
        view.clearAnimation()
        view.animate().cancel()
        view.rotation = 0f
        view.scaleX = 1f
        view.scaleY = 1f
        view.alpha = 1f

        when (animType.lowercase().trim()) {
            // 🏮 حركة التأرجح (مناسبة للفانوس المعلق)
            "swing" -> {
                // تغيير نقطة الارتكاز لتكون في منتصف الحافة العليا (كأنه معلق بخيط)
                view.pivotX = view.width / 2f
                view.pivotY = 0f

                // التدوير يميناً ويساراً
                val rotate = ObjectAnimator.ofFloat(view, "rotation", -10f, 10f)
                rotate.duration = 2000 // ثانيتين
                rotate.repeatMode = ObjectAnimator.REVERSE
                rotate.repeatCount = ObjectAnimator.INFINITE
                rotate.interpolator = AccelerateDecelerateInterpolator() // حركة ناعمة في الأطراف
                rotate.start()
            }

            // ✨ حركة الإضاءة/النبض (تكبير وتصغير مع شفافية)
            "glow" -> {
                // تكبير وتصغير
                val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.1f)
                val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.1f)
                // تغيير الشفافية (كأنه ضوء يخفت ويقوى)
                val alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 1.0f, 0.6f)

                val animator = ObjectAnimator.ofPropertyValuesHolder(view, scaleX, scaleY, alpha)
                animator.duration = 1500
                animator.repeatCount = ObjectAnimator.INFINITE
                animator.repeatMode = ObjectAnimator.REVERSE
                animator.start()
            }

            // 🔄 دوران كامل (مثل مروحة)
            "spin" -> {
                val rotate = ObjectAnimator.ofFloat(view, "rotation", 0f, 360f)
                rotate.duration = 5000
                rotate.repeatCount = ObjectAnimator.INFINITE
                rotate.interpolator = LinearInterpolator()
                rotate.start()
            }
        }
    }
}