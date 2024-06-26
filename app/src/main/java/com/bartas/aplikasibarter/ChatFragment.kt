package com.bartas.aplikasibarter

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bartas.aplikasibarter.Adapter.ChatAdapter
import com.bartas.aplikasibarter.Models.DataChatMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.Locale

class ChatFragment : Fragment(), ChatAdapter.ChatItemClickListener {

    private lateinit var chatAdapter: ChatAdapter
    private val chatList: MutableList<DataChatMessage> = mutableListOf()
    private lateinit var databaseReference: DatabaseReference
    private lateinit var currentUserUid: String
    private lateinit var searchView: SearchView
    private lateinit var filteredChatList: MutableList<DataChatMessage>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView: RecyclerView = view.findViewById(R.id.userRecyclerView)
        chatAdapter = ChatAdapter(chatList, this)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = chatAdapter
        }

        val auth = FirebaseAuth.getInstance()
        currentUserUid = auth.currentUser?.uid ?: ""

        databaseReference = FirebaseDatabase.getInstance().reference.child("Pengguna")
        getUsersFromFirebase()

        searchView = view.findViewById(R.id.searchView)
        filteredChatList = mutableListOf()

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterChatList(newText)
                return true
            }
        })
    }

    private fun filterChatList(query: String?) {
        filteredChatList.clear()

        if (query.isNullOrEmpty()) {
            filteredChatList.addAll(chatList)
        } else {
            val lowerCaseQuery = query.toLowerCase(Locale.getDefault())
            for (item in chatList) {
                if (item.userName?.toLowerCase(Locale.getDefault())?.contains(lowerCaseQuery) == true) {
                    filteredChatList.add(item)
                }
            }
        }

        chatAdapter.submitList(filteredChatList)
        chatAdapter.notifyDataSetChanged()
    }

    private fun getUsersFromFirebase() {
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (postSnapshot in snapshot.children) {
                    val userId = postSnapshot.key
                    if (userId != currentUserUid) {
                        val name = postSnapshot.child("username").value.toString()
                        val photoUrl = postSnapshot.child("photo_url").value.toString()

                        val chatMessage = DataChatMessage(
                            senderUid = currentUserUid,
                            receiverUid = userId ?: "",
                            userName = name,
                            message = "",
                            timestamp = "",  // Replace this with the correct timestamp
                            photoUrl = photoUrl,
                        )

                        chatList.add(chatMessage)
                    }
                }
                chatAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle errors here
            }
        })
    }

    override fun onChatItemClick(dataUser: DataChatMessage) {
        val intent = Intent(context, BukaChatActivity::class.java)
        intent.putExtra("username", dataUser.userName)
        intent.putExtra("photo_url", dataUser.photoUrl)
        intent.putExtra("receiverUid", dataUser.receiverUid)
        intent.putExtra("lastActiveTime", dataUser.timestamp)
        startActivity(intent)
    }
}


