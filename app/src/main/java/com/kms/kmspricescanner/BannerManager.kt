package com.kms.kmspricescanner

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

// ✅ كلاس مستقل مسؤوليته الوحيدة (Single Responsibility) هي إدارة البانر المتحرك
class BannerManager(
    private val context: Context,
    private val bannerViewPager: ViewPager2
) {
    private val bannerImages = ArrayList<Any>()
    private val sliderHandler = Handler(Looper.getMainLooper())
    private var sliderRunnable: Runnable? = null

    init {
        setupBannerSlider()
    }

    private fun setupBannerSlider() {
        val adapter = BrandsAdapter(bannerImages)
        bannerViewPager.adapter = adapter
        bannerViewPager.clipToPadding = false
        bannerViewPager.clipChildren = false
        bannerViewPager.offscreenPageLimit = 3
        bannerViewPager.getChildAt(0).overScrollMode = RecyclerView.OVER_SCROLL_NEVER

        sliderRunnable = Runnable {
            if (bannerImages.size > 1) {
                var nextItem = bannerViewPager.currentItem + 1
                if (nextItem >= bannerImages.size) nextItem = 0
                bannerViewPager.setCurrentItem(nextItem, true)
            }
            // إعادة استدعاء نفسه بعد 4 ثوانٍ
            sliderRunnable?.let { sliderHandler.postDelayed(it, 9000) }
        }
    }

    fun loadCachedBanners() {
        val prefs = context.getSharedPreferences("KMS_Prefs", Context.MODE_PRIVATE)
        val cachedString = prefs.getString("cached_banners", null)
        bannerImages.clear()
        if (!cachedString.isNullOrEmpty()) {
            val urls = cachedString.split(",")
            for (url in urls) {
                if (url.trim().isNotEmpty()) bannerImages.add(url.trim())
            }
        } else {
            bannerImages.add(R.drawable.company_logo)
        }
        bannerViewPager.adapter?.notifyDataSetChanged()
    }

    fun updateBanners(urls: List<String>) {
        bannerImages.clear()
        bannerImages.addAll(urls)
        bannerViewPager.adapter?.notifyDataSetChanged()
    }

    fun startSlider() {
        sliderRunnable?.let { sliderHandler.postDelayed(it, 4000) }
    }

    // 🔥 الدالة الأهم: تدمير العملية في الخلفية لمنع تسرب الذاكرة (Memory Leak)
    fun stopSlider() {
        sliderRunnable?.let { sliderHandler.removeCallbacks(it) }
    }
}