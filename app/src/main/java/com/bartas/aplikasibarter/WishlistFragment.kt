package com.bartas.aplikasibarter

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bartas.aplikasibarter.Adapter.MyAdapterWishlist
import com.bartas.aplikasibarter.Models.DataClassItem
import com.bartas.aplikasibarter.databinding.FragmentWishlistBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class WishlistFragment : Fragment() {

    private lateinit var binding: FragmentWishlistBinding
    private lateinit var adapter: MyAdapterWishlist

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentWishlistBinding.inflate(inflater, container, false)
        val view = binding.root

        val user = FirebaseAuth.getInstance().currentUser
        val userId = user?.uid

        val recyclerView = binding.wishlRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val sharedPreferences = requireContext().getSharedPreferences("wishlist_pref_${userId}", Context.MODE_PRIVATE)
        adapter = MyAdapterWishlist(requireContext(), emptyList(), sharedPreferences)
        recyclerView.adapter = adapter

        userId?.let {
            val wishlistReference = FirebaseDatabase.getInstance().getReference("Wishlist").child(it)
            wishlistReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val wishlistItems = mutableListOf<DataClassItem>()
                    for (itemSnapshot in snapshot.children) {
                        val item = itemSnapshot.getValue(DataClassItem::class.java)
                        item?.let {
                            wishlistItems.add(it)
                        }
                    }
                    adapter.updateWishlistData(userId, wishlistItems)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseError", "Failed to fetch wishlist data: ${error.message}")
                }
            })
        }

        return view
    }
}
