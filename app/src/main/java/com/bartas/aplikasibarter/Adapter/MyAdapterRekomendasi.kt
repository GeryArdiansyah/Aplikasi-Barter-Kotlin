package com.bartas.aplikasibarter

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
import com.bartas.aplikasibarter.Models.DataClassItem
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MyAdapterRekomendasi(private val context: Context, private var dataList: List<DataClassItem>) : RecyclerView.Adapter<MyViewHolderRek>() {

    val database = FirebaseDatabase.getInstance()
    val reference = database.getReference("Wishlist")
    private lateinit var auth : FirebaseAuth
    private lateinit var uid : String

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolderRek {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.recycler_item_rekomendasi, parent, false)
        return MyViewHolderRek(view)
    }

    override fun onBindViewHolder(holder: MyViewHolderRek, position: Int) {
        val colorStateList = AppCompatResources.getColorStateList(context, R.color.selector_favorit)
        CompoundButtonCompat.setButtonTintList(holder.favorite, colorStateList)
        Glide.with(context).load(dataList[position].imageUrl)
            .into(holder.Image)
        holder.Merk.text = dataList[position].nama

        val hargaRupiah = dataList[position].harga?.let { convertToRupiah(it) }
        holder.Harga.text = hargaRupiah

        auth = FirebaseAuth.getInstance()
        uid = auth.currentUser?.uid.toString()

        holder.favorite.isChecked = dataList[position].favorite
        val userId = uid
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
            Log.d("FirebaseSuccess", "Item clicked: ${dataList[position].id}")
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
class MyViewHolderRek(itemView: View) : RecyclerView.ViewHolder(itemView) {
    var Image: ImageView
    var Merk: TextView
    var Harga: TextView
    var Card: CardView
    var favorite: CheckBox
    init {
        Image = itemView.findViewById(R.id.recImage)
        Merk = itemView.findViewById(R.id.recMerk)
        Harga = itemView.findViewById(R.id.recHarga)
        Card = itemView.findViewById(R.id.recCard)
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