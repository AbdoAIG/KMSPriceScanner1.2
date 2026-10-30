package com.kms.kmspricescanner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

class ProductRecyclerAdapter(
    private var products: List<ProductEntity>,
    private val onItemClick: (ProductEntity) -> Unit
) : RecyclerView.Adapter<ProductRecyclerAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tv_product_name_item)
        val tvCategory: TextView = itemView.findViewById(R.id.tv_product_category_item)
        val tvPrice: TextView = itemView.findViewById(R.id.tv_product_price_item)
        val imgProduct: ImageView = itemView.findViewById(R.id.img_product_item)

        init {
            // التعامل مع النقر باحترافية وتوافق تام مع جميع الإصدارات
            itemView.setOnClickListener {
                @Suppress("DEPRECATION")
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(products[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        holder.tvName.text = product.name
        holder.tvCategory.text = "${product.department} - ${product.branch}"
        holder.tvPrice.text = product.pricePiece

        if (product.imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(product.imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.company_logo)
                .into(holder.imgProduct)
        } else {
            holder.imgProduct.setImageResource(R.drawable.company_logo)
        }
    }

    override fun getItemCount(): Int = products.size

    fun updateData(newProducts: List<ProductEntity>) {
        this.products = newProducts
        notifyDataSetChanged()
    }
}