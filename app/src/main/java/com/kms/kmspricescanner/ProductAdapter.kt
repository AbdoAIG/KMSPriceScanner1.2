package com.kms.kmspricescanner

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

class ProductAdapter(context: Context, private val products: List<ProductEntity>) :
    ArrayAdapter<ProductEntity>(context, 0, products) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.list_item_product, parent, false)
        }

        val product = products[position]

        val tvName = view!!.findViewById<TextView>(R.id.tv_product_name_item)
        val tvCategory = view.findViewById<TextView>(R.id.tv_product_category_item)
        val tvPrice = view.findViewById<TextView>(R.id.tv_product_price_item)
        val imgProduct = view.findViewById<ImageView>(R.id.img_product_item)

        tvName.text = product.name
        tvCategory.text = "${product.department} - ${product.branch}"
        tvPrice.text = product.pricePiece

        if (product.imageUrl.isNotEmpty()) {
            Glide.with(context)
                .load(product.imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.company_logo)
                .into(imgProduct)
        } else {
            imgProduct.setImageResource(R.drawable.company_logo)
        }

        return view
    }
}