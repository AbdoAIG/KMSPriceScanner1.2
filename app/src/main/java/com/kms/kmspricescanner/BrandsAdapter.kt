package com.kms.kmspricescanner

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterInside

class BrandsAdapter(private val imagesList: List<Any>) :
    RecyclerView.Adapter<BrandsAdapter.BrandViewHolder>() {

    // ✅ التعديل الأول: استقبال الصورة مباشرة دون البحث عنها بـ ID
    inner class BrandViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // بما أن العنصر هو نفسه الصورة، نقوم بتحويله (Casting) مباشرة
        val imgBanner: ImageView = itemView as ImageView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BrandViewHolder {
        // إنشاء عنصر الصورة برمجياً
        val imageView = ImageView(parent.context)

        // ضبط أبعاد الصورة لتملأ الـ CardView
        imageView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        // ضبط طريقة العرض
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        imageView.setBackgroundColor(android.graphics.Color.parseColor("#FFFFFF"))

        // ✅ التعديل الثاني: حذفنا سطر (imageView.id = ...) لأنه سبب المشكلة

        return BrandViewHolder(imageView)
    }

    override fun onBindViewHolder(holder: BrandViewHolder, position: Int) {
        val image = imagesList[position]

        // تحميل الصورة باستخدام Glide
        Glide.with(holder.itemView.context)
            .load(image)
            .transform(CenterInside())
            .into(holder.imgBanner)
    }

    override fun getItemCount(): Int {
        return imagesList.size
    }
}