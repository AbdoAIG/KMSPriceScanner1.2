package com.kms.kmspricescanner

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners

class ProductAdapter(private val context: Context, private val dataSource: List<Product>) : BaseAdapter() {

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getCount(): Int {
        return dataSource.size
    }

    override fun getItem(position: Int): Any {
        return dataSource[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        // 1. استخدام التصميم الجديد المحسن
        val rowView = convertView ?: inflater.inflate(R.layout.list_item_product, parent, false)

        // 2. الحصول على المنتج الحالي
        val product = getItem(position) as Product

        // 3. ربط العناصر (تمت إضافة عنصر التصنيف الجديد)
        val tvName = rowView.findViewById<TextView>(R.id.tv_product_name_item)
        val tvCategory = rowView.findViewById<TextView>(R.id.tv_product_category_item) // ✅ العنصر الجديد
        val tvPrice = rowView.findViewById<TextView>(R.id.tv_product_price_item)
        val imgProduct = rowView.findViewById<ImageView>(R.id.img_product_item)

        // 4. تعبئة البيانات
        tvName.text = product.name

        // ✅ عرض القسم تحت الاسم (يمكنك تغييرها لـ product.localBarcode لو فضلت عرض الباركود)
        tvCategory.text = product.department

        // عرض السعر
        tvPrice.text = product.pricePiece

        // 5. تحميل الصورة وتنسيقها لتناسب المربع الجديد
        if (product.imageUrl.isNotEmpty() && product.imageUrl != "null") {
            try {
                Glide.with(context)
                    .load(product.imageUrl)
                    // ✅ CenterCrop لملء المربع، و RoundedCorners 12 لمطابقة التصميم
                    .transform(CenterCrop(), RoundedCorners(12))
                    .placeholder(R.drawable.company_logo)
                    .error(R.drawable.company_logo)
                    .into(imgProduct)
            } catch (e: Exception) {
                imgProduct.setImageResource(R.drawable.company_logo)
            }
        } else {
            // في حال عدم وجود صورة، نضع الشعار
            imgProduct.setImageResource(R.drawable.company_logo)
        }

        return rowView
    }
}