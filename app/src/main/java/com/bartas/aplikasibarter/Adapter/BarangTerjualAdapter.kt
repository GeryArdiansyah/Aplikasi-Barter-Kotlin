package com.bartas.aplikasibarter.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bartas.aplikasibarter.Models.DataClassItem
import com.bumptech.glide.Glide
import com.bartas.aplikasibarter.R

class BarangTerjualAdapter(private var soldBarangList: List<DataClassItem>) :
    RecyclerView.Adapter<BarangTerjualAdapter.SoldBarangViewHolder>() {

    inner class SoldBarangViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemName: TextView = itemView.findViewById(R.id.soldmerk)
        val itemPrice: TextView = itemView.findViewById(R.id.soldharga)
        val itemImage: ImageView = itemView.findViewById(R.id.imagesold)
        val stikerSold: ImageView = itemView.findViewById(R.id.stikersold)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SoldBarangViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycler_barang_terjual, parent, false)
        return SoldBarangViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: SoldBarangViewHolder, position: Int) {
        val currentItem = soldBarangList[position]

        holder.itemName.text = currentItem.nama
        val hargaRupiah = currentItem.harga?.let { convertToRupiah(it) }
        holder.itemPrice.text = hargaRupiah

        // Tampilkan gambar menggunakan Glide
        Glide.with(holder.itemView.context)
            .load(currentItem.imageUrl)
            .into(holder.itemImage)

        // Tampilkan stiker "sold" pada item yang sudah terjual
        holder.stikerSold.visibility = View.VISIBLE // Tampilkan stiker sold
    }

    override fun getItemCount() = soldBarangList.size

    fun updateData(newSoldBarangList: List<DataClassItem>) {
        soldBarangList = newSoldBarangList
        notifyDataSetChanged()
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



