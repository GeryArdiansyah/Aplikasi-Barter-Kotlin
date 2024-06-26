package com.bartas.aplikasibarter.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bartas.aplikasibarter.Models.DataChatMessage
import com.bumptech.glide.Glide
import com.bartas.aplikasibarter.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(
    private var chatList: List<DataChatMessage>,
    private val listener: ChatItemClickListener
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userName: TextView = itemView.findViewById(R.id.userName)
        val deskripsi: TextView = itemView.findViewById(R.id.deskripsi)
        val profil: ImageView = itemView.findViewById(R.id.profil)
        val jamAktif: TextView = itemView.findViewById(R.id.jam) // Tambahkan TextView untuk jam aktif
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_chat, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val currentItem = chatList[position]

        holder.userName.text = currentItem.userName

        // Menampilkan deskripsi pesan terakhir jika belum ada obrolan sebelumnya
        holder.deskripsi.text = if (currentItem.message.isNullOrEmpty()) {
            "Hai, Terima kasih sudah mengunjungi Profil..."
        } else {
            currentItem.message
        }

        Glide.with(holder.profil.context)
            .load(currentItem.photoUrl)
            .placeholder(R.drawable.foto_profil)
            .into(holder.profil)

        // Menampilkan jam aktif (current time)
        val currentTimeMillis = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val currentFormattedTime = dateFormat.format(Date(currentTimeMillis))
        holder.jamAktif.text = currentFormattedTime


        holder.itemView.setOnClickListener {
            listener.onChatItemClick(currentItem)
        }
    }

    fun submitList(newList: List<DataChatMessage>) {
        chatList = newList
    }

    override fun getItemCount(): Int {
        return chatList.size
    }

    interface ChatItemClickListener {
        fun onChatItemClick(dataUser: DataChatMessage)
    }
}

