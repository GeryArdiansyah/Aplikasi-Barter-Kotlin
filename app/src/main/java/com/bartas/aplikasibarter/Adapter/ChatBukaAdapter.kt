package com.bartas.aplikasibarter.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bartas.aplikasibarter.Models.DataChatMessage
import com.bartas.aplikasibarter.R
import com.bumptech.glide.Glide

class ChatBukaAdapter(private val currentUserUid: String) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val chatMessages = mutableListOf<DataChatMessage>()

    companion object {
        const val MESSAGE_TYPE_SENT = 1
        const val MESSAGE_TYPE_RECEIVED = 2
    }

    inner class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textMessage: TextView = itemView.findViewById(R.id.tv_chat_kirim)
        val textTimestamp: TextView = itemView.findViewById(R.id.tv_timestamp_kirim)
        val imageMessage: ImageView = itemView.findViewById(R.id.img_chat_kirim)
    }

    inner class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textMessage: TextView = itemView.findViewById(R.id.tv_chat_terima)
        val textTimestamp: TextView = itemView.findViewById(R.id.tv_timestamp_terima)
        val imageMessage: ImageView = itemView.findViewById(R.id.img_chat_terima)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            MESSAGE_TYPE_SENT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_kirim, parent, false)
                SentMessageViewHolder(view)
            }
            MESSAGE_TYPE_RECEIVED -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_terima, parent, false)
                ReceivedMessageViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = chatMessages[position]

        when (holder) {
            is SentMessageViewHolder -> {
                holder.textMessage.text = currentItem.message
                holder.textTimestamp.text = currentItem.timestamp
                if (currentItem.photoUrl != null) {
                    holder.imageMessage.visibility = View.VISIBLE
                    Glide.with(holder.itemView.context)
                        .load(currentItem.photoUrl)
                        .into(holder.imageMessage)
                } else {
                    holder.imageMessage.visibility = View.GONE
                }
            }
            is ReceivedMessageViewHolder -> {
                holder.textMessage.text = currentItem.message
                holder.textTimestamp.text = currentItem.timestamp
                if (currentItem.photoUrl != null) {
                    holder.imageMessage.visibility = View.VISIBLE
                    Glide.with(holder.itemView.context)
                        .load(currentItem.photoUrl)
                        .into(holder.imageMessage)
                } else {
                    holder.imageMessage.visibility = View.GONE
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (chatMessages[position].senderUid == currentUserUid) {
            MESSAGE_TYPE_SENT
        } else {
            MESSAGE_TYPE_RECEIVED
        }
    }

    override fun getItemCount(): Int {
        return chatMessages.size
    }

    fun addMessage(message: DataChatMessage) {
        chatMessages.add(message)
        notifyItemInserted(chatMessages.size - 1)
    }
}
