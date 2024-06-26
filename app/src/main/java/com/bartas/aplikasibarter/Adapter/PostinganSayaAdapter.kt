package com.bartas.aplikasibarter

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.RecyclerView
import com.bartas.aplikasibarter.Models.DataClassPosting
import com.bumptech.glide.Glide
import com.google.firebase.database.FirebaseDatabase

class PostinganSayaAdapter : RecyclerView.Adapter<PostinganSayaAdapter.ViewHolder>() {

    private var postinganList: List<DataClassPosting> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recycler_postingan_saya, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val postingan = postinganList[position]

        Glide.with(holder.itemView.context)
            .load(postingan.imageUrl)
            .into(holder.itemView.findViewById<ImageView>(R.id.imagepost))

        holder.itemView.findViewById<TextView>(R.id.txtbrand).text = postingan.nama
        holder.itemView.findViewById<TextView>(R.id.txtprice).text = convertToRupiah(postingan.harga)

        holder.itemView.findViewById<AppCompatButton>(R.id.btnsoldout).setOnClickListener {
            // Get the reference to the "postingbarang" node
            val postinganRef = FirebaseDatabase.getInstance().getReference("postingbarang").child(postingan.id)

            // Set status barang to "sold"
            postinganRef.child("status").setValue("sold")
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Move the item to "soldout" node
                        val soldBarangRef = FirebaseDatabase.getInstance().getReference("soldout").push()
                        soldBarangRef.setValue(postingan)
                            .addOnCompleteListener { soldTask ->
                                if (soldTask.isSuccessful) {
                                    // Remove the item from the "postingbarang" node
                                    postinganRef.removeValue()
                                        .addOnCompleteListener { removeTask ->
                                            if (removeTask.isSuccessful) {
                                                // Item removed from the list successfully
                                                val newList = postinganList.toMutableList()
                                                newList.remove(postingan)
                                                postinganList = newList.toList()
                                                notifyDataSetChanged()

                                                // Redirect to the history page
                                                redirectToRiwayat(holder.itemView.context)
                                            } else {
                                                // Failed to remove the item
                                                Toast.makeText(
                                                    holder.itemView.context,
                                                    "Gagal menghapus barang",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                } else {
                                    // Failed to move the item to "soldout"
                                    Toast.makeText(
                                        holder.itemView.context,
                                        "Gagal memindahkan barang",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    } else {
                        // Failed to update status
                        Toast.makeText(
                            holder.itemView.context,
                            "Gagal mengubah status barang",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }



        holder.itemView.findViewById<AppCompatButton>(R.id.btnhapus).setOnClickListener {
            val context = holder.itemView.context

            val dialog = Dialog(context)
            dialog.setContentView(R.layout.popup_hapus)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.setCancelable(false)

            val btnYes = dialog.findViewById<AppCompatButton>(R.id.btnyeshapus)
            val btnNo = dialog.findViewById<AppCompatButton>(R.id.btnnohapus)

            btnYes.setOnClickListener {
                val databaseReference = FirebaseDatabase.getInstance().getReference("postingbarang")
                val postinganId = postingan.id

                if (postinganId != null) {
                    databaseReference.child(postinganId).removeValue()
                        .addOnSuccessListener {
                            val newList = ArrayList(postinganList)
                            newList.remove(postingan)
                            postinganList = newList.toList()
                            notifyDataSetChanged()

                            // Tampilkan popup sukses
                            val dialogSuccess = Dialog(context)
                            dialogSuccess.setContentView(R.layout.popup_sukses_hapus)
                            dialogSuccess.window?.setBackgroundDrawableResource(android.R.color.transparent)
                            dialogSuccess.setCancelable(false)

                            val btnOk = dialogSuccess.findViewById<AppCompatButton>(R.id.btnokayhapus)

                            btnOk.setOnClickListener {
                                dialogSuccess.dismiss()
                            }
                            dialogSuccess.show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                context,
                                "Gagal menghapus postingan: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }

                dialog.dismiss()
            }

            btnNo.setOnClickListener {
                dialog.dismiss()
            }
            dialog.show()
        }

        holder.itemView.findViewById<AppCompatButton>(R.id.btnubah).setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, UbahBarangActivity::class.java)
            intent.putExtra("POSTINGAN_ID", postinganList[holder.adapterPosition].id)

            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return postinganList.size
    }

    fun submitList(list: List<DataClassPosting>) {
        postinganList = list
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}

private fun redirectToRiwayat(context: Context) {
    val intent = Intent(context, BarangTerjualActivity::class.java)
    context.startActivity(intent)
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
