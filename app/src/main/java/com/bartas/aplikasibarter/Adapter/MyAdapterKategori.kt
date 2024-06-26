package com.bartas.aplikasibarter.Adapter

import android.content.Context
import android.content.Intent
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
import com.bartas.aplikasibarter.DetailActivity
import com.bartas.aplikasibarter.Models.DataClassItem
import com.bartas.aplikasibarter.R
import com.bumptech.glide.Glide
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class MyAdapterKategori(private val context: Context, private var dataList: List<DataClassItem>, private val databaseReference: DatabaseReference?) :
    RecyclerView.Adapter<MyViewHolder>() {

    val database = FirebaseDatabase.getInstance()
    val reference = database.getReference("Wishlist")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view: View =
            LayoutInflater.from(parent.context).inflate(R.layout.recycler_search_kategori, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val colorStateList = AppCompatResources.getColorStateList(context, R.color.selector_favorit)
        CompoundButtonCompat.setButtonTintList(holder.favorite, colorStateList)
        
        Glide.with(context).load(dataList[position].imageUrl)
            .into(holder.Image)

        holder.Merk.text = dataList[position].nama

        val hargaRupiah = dataList[position].harga?.let { convertToRupiah(it) }
        holder.Harga.text = hargaRupiah

        holder.favorite.isChecked = dataList[position].favorite
        val userId = dataList[position].userId
        val id = dataList[position].id
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("wishlist_pref_${userId}", Context.MODE_PRIVATE)
        val isChecked = sharedPreferences.getBoolean(dataList[position].id, false)
        holder.favorite.isChecked = isChecked

        holder.favorite.setOnCheckedChangeListener { _, isChecked ->
            dataList[position].favorite = isChecked

            sharedPreferences.edit {
                putBoolean(dataList[position].id, isChecked)
                apply()
            }
                if (userId != null && id != null) {
                    if (isChecked) {
                        reference.child(userId).child(id).setValue(dataList[position])
                            .addOnSuccessListener {
                                Log.d("FirebaseSuccess", "Favorite status updated successfully")
                                Toast.makeText(context, "Barang ditambahkan ke Wishlist", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                Log.e("FirebaseError", "Failed to update favorite status: ${it.message}")
                            }
                    }else if (!isChecked){
                        reference.child(userId).child(id).removeValue()
                            .addOnSuccessListener {
                                Log.d("FirebaseSuccess", "Item removed from Wishlist successfully")
                                Toast.makeText(context, "Barang dihapus dari Wishlist", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                Log.e("FirebaseError", "Failed to remove item from Wishlist: ${it.message}")
                            }
                    }
                } else {
                    val errorMessage = "userId or itemName is null - userId: $userId, itemName: $id"
                    Log.e("FirebaseError", errorMessage)
                }
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(context, DetailActivity::class.java)
            intent.putExtra("itemId", dataList[position].id)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    fun searchDataList(searchList: List<DataClassItem>) {
        dataList = searchList
        notifyDataSetChanged()
    }
}

class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    var Image: ImageView
    var Merk: TextView
    var Harga: TextView
    var Card: CardView
    var favorite: CheckBox

    init {
        Image = itemView.findViewById(R.id.image)
        Merk = itemView.findViewById(R.id.merk)
        Harga = itemView.findViewById(R.id.harga)
        Card = itemView.findViewById(R.id.cardView)
        favorite = itemView.findViewById(R.id.imageViewFavorite)
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