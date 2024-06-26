package com.bartas.aplikasibarter.Adapter

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.cardview.widget.CardView
import androidx.core.content.edit
import androidx.core.widget.CompoundButtonCompat
import androidx.recyclerview.widget.RecyclerView
import com.bartas.aplikasibarter.Models.DataClassItem
import com.bartas.aplikasibarter.R
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MyAdapterWishlist(
    private val context: Context,
    private var wishlistDataList: List<DataClassItem>,
    private val sharedPreferences: SharedPreferences
) : RecyclerView.Adapter<MyAdapterWishlist.MyViewHolder>() {

    val database = FirebaseDatabase.getInstance()
    val reference = database.getReference("Wishlist")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycler_wishlist, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val colorStateList = AppCompatResources.getColorStateList(context, R.color.selector_favorit)
        CompoundButtonCompat.setButtonTintList(holder.favorite, colorStateList)

        Log.d("MyAdapterWishlist", "Binding data for position: $position")

        Glide.with(context)
            .load(wishlistDataList[position].imageUrl)
            .placeholder(R.drawable.fashion_nb_997)
            .into(holder.Image)
        Log.d("MyAdapterWishlist", "Image URL: ${wishlistDataList[position].imageUrl}")

        holder.Merk.text = wishlistDataList[position].nama

        val hargaRupiah = wishlistDataList[position].harga?.let { convertToRupiah(it) }
        holder.Harga.text = hargaRupiah

        holder.favorite.isChecked = wishlistDataList[position].favorite

        holder.favorite.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {
                wishlistDataList[position].favorite = true

                val userId = FirebaseAuth.getInstance().currentUser?.uid
                val idbarang = wishlistDataList[position].id

                userId?.let {
                    idbarang?.let { idbarang ->
                        val wishlistReference = reference.child(it).child(idbarang)
                        wishlistReference.removeValue()
                            .addOnSuccessListener {
                                Log.d("FirebaseSuccess", "Item removed from wishlist successfully")
                                sharedPreferences.edit {
                                    remove(idbarang)
                                    apply()
                                }
                                Toast.makeText(context, "Barang dihapus dari Wishlist", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                Log.e("FirebaseError", "Failed to remove item from wishlist: ${it.message}")
                            }
                    }
                } ?: run {
                    Log.e("FirebaseError", "User is not logged in")
                }
            } else {
                // Handle case when checkbox is unchecked (if needed)
            }
        }
    }

    override fun getItemCount(): Int {
        return wishlistDataList.size
    }

    fun updateWishlistData(userId: String, wishlistList: List<DataClassItem>) {
        wishlistDataList = wishlistList
        notifyDataSetChanged()
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var Image: ImageView
        var Merk: TextView
        var Harga: TextView
        var Card: CardView
        var favorite: CheckBox

        init {
            Image = itemView.findViewById(R.id.imagewishlist)
            Merk = itemView.findViewById(R.id.merkwishlist)
            Harga = itemView.findViewById(R.id.hargawishlist)
            Card = itemView.findViewById(R.id.cardViewwishlist)
            favorite = itemView.findViewById(R.id.buttonwishlist)
        }
    }

    private fun convertToRupiah(harga: String): String {
        val formattedHarga = StringBuilder()
        val reversedHarga = harga.reversed()
        for (i in 0 until reversedHarga.length) {
            formattedHarga.append(reversedHarga[i])
            if ((i + 1) % 3 == 0 && (i + 1) != reversedHarga.length) {
                formattedHarga.append('.')
            }
        }
        return "Rp${formattedHarga.reverse()}"
    }
}
