package com.kms.kmspricescanner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CartRecyclerAdapter(
    private var cartList: List<CartItem>,
    private val onQuantityChange: (CartItem, Int) -> Unit
) : RecyclerView.Adapter<CartRecyclerAdapter.CartViewHolder>() {

    inner class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tv_cart_item_name)
        val tvUnitPrice: TextView = itemView.findViewById(R.id.tv_cart_item_unit_price)
        val tvTotal: TextView = itemView.findViewById(R.id.tv_cart_item_total)
        val tvQty: TextView = itemView.findViewById(R.id.tv_cart_item_qty)
        val btnPlus: ImageButton = itemView.findViewById(R.id.btn_increase_item)
        val btnMinus: ImageButton = itemView.findViewById(R.id.btn_decrease_item)
        val btnRemove: ImageButton = itemView.findViewById(R.id.btn_remove_item)

        init {
            btnPlus.setOnClickListener {
                @Suppress("DEPRECATION")
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = cartList[position]
                    onQuantityChange(item, item.quantity + 1)
                }
            }

            btnMinus.setOnClickListener {
                @Suppress("DEPRECATION")
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = cartList[position]
                    onQuantityChange(item, item.quantity - 1)
                }
            }

            // تفعيل زر الحذف المباشر
            btnRemove.setOnClickListener {
                @Suppress("DEPRECATION")
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = cartList[position]
                    onQuantityChange(item, 0) // الكمية 0 تعني حذف العنصر من السلة
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_cart, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val item = cartList[position]

        holder.tvName.text = item.product.name
        holder.tvQty.text = item.quantity.toString()

        // استخراج سعر القطعة وعرضه
        val unitPrice = item.product.pricePiece.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
        holder.tvUnitPrice.text = String.format(java.util.Locale.US, "سعر الوحدة: %.2f ج.م", unitPrice)

        // عرض السعر الإجمالي للصنف (السعر × الكمية)
        val itemTotal = item.getTotalPrice()
        holder.tvTotal.text = String.format(java.util.Locale.US, "%.2f", itemTotal)
    }

    override fun getItemCount(): Int = cartList.size

    fun updateData(newList: List<CartItem>) {
        this.cartList = newList
        notifyDataSetChanged()
    }
}