package com.bartas.aplikasibarter

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.Toolbar
import androidx.core.widget.CompoundButtonCompat
import androidx.room.util.copy
import com.bartas.aplikasibarter.Models.DataChatMessage
import com.bartas.aplikasibarter.Models.DataClassItem
import com.bartas.aplikasibarter.Models.DataClassPengguna
import com.bartas.aplikasibarter.databinding.ActivityDetailBarangBinding
import com.bartas.aplikasibarter.databinding.ActivityEditProfilBinding
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class DetailActivity : AppCompatActivity() {

    private lateinit var binding:ActivityDetailBarangBinding
    private lateinit var databaseReference: DatabaseReference
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var toolbar: Toolbar
    private lateinit var dataClassItem: DataClassItem
    private lateinit var userposting: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBarangBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        toolbar = findViewById<Toolbar>(R.id.toolbarBack)
        toolbar.setNavigationOnClickListener {
            finish()
        }


        firebaseAuth = FirebaseAuth.getInstance()
        val itemId = intent.getStringExtra("itemId") ?: ""

        binding.llChatPelapak.setOnClickListener {
            val intent = Intent(this, BukaChatActivity::class.java)
            startActivity(intent)
        }



        // Setel referensi database
        databaseReference = FirebaseDatabase.getInstance().reference.child("postingbarang")

        // Ambil referensi elemen UI
        val itemImageView: ImageView = findViewById(R.id.imageViewBarang)
        val itemNameTextView: TextView = findViewById(R.id.textView3Nama)
        val itemPriceTextView: TextView = findViewById(R.id.textViewHrg)
        val itemDescriptionTextView: TextView = findViewById(R.id.textView7)
        val pelapakNameTextView: TextView = findViewById(R.id.textViewpelapak)
        val pelapakLocationTextView: TextView = findViewById(R.id.textView2)

        // Tampilkan gambar terlebih dahulu
        queryDetail(
            itemId,
            itemImageView,
            itemNameTextView,
            itemPriceTextView,
            itemDescriptionTextView,
            pelapakNameTextView,
            pelapakLocationTextView,
            binding.imageViewFavorite
        )
        val favoriteCheckBox: CheckBox = findViewById(R.id.imageViewFavorite)
        val colorStateList = AppCompatResources.getColorStateList(this, R.color.selector_favorit)
        CompoundButtonCompat.setButtonTintList(favoriteCheckBox, colorStateList)

        binding.llChatPelapak.setOnClickListener {
            val intent = Intent(this, BukaChatActivity::class.java)
            intent.putExtra("receiverUid", userposting)
            startActivity(intent)
        }
    }

    private fun queryDetail(
        itemId: String,
        itemImageView: ImageView,
        itemNameTextView: TextView,
        itemPriceTextView: TextView,
        itemDescriptionTextView: TextView,
        pelapakNameTextView: TextView,
        pelapakLocationTextView: TextView,
        favoriteCheckBox: CheckBox) {
        val query: Query = databaseReference.orderByChild("id").equalTo(itemId)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (postSnapshot in snapshot.children) {
                        val imageUrl = postSnapshot.child("imageUrl").value.toString()
                        val itemName = postSnapshot.child("nama").value.toString()
                        val itemPrice = postSnapshot.child("harga").value.toString()
                        val itemDescription = postSnapshot.child("deskripsi").value.toString()
                        val pelapakName = postSnapshot.child("username").value.toString()
                        val pelapakLocation = postSnapshot.child("lokasi").value.toString()
                        val isFavorite = getFavoriteStatus(itemId)
                        userposting = postSnapshot.child("uid").value.toString()
                        Log.d("userposting", "User Posting : $userposting")
                        favoriteCheckBox.isChecked = isFavorite

                        // Tampilkan data ke elemen UI
                        itemNameTextView.text = itemName
                        itemPriceTextView.text = convertToRupiah(itemPrice)
                        itemDescriptionTextView.text = itemDescription
                        pelapakNameTextView.text = "Pelapak : $pelapakName"
                        pelapakLocationTextView.text = pelapakLocation

                        // Load gambar ke ImageView menggunakan Glide atau metode lainnya
                        if (imageUrl.isNotEmpty()) {
                            Glide.with(this@DetailActivity)
                                .load(imageUrl)
                                .into(itemImageView)
                        }
                        favoriteCheckBox.setOnCheckedChangeListener { _, isChecked ->
                            updateFavoriteStatus(itemId, isChecked)
                        }
                    }
                } else {
                    // Tampilkan pesan jika data tidak ditemukan
                    Toast.makeText(this@DetailActivity, "Data tidak ditemukan", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Tangani kesalahan saat mengambil data
                Toast.makeText(this@DetailActivity, "Gagal mengambil data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
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

    private fun updateFavoriteStatus(itemId: String, isFavorite: Boolean) {
        val userId = firebaseAuth.currentUser?.uid

        if (userId != null) {
            val reference = FirebaseDatabase.getInstance().reference.child("Wishlist").child(userId)

            if (isFavorite) {
                databaseReference.child(itemId).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val dataClassItem = snapshot.getValue(DataClassItem::class.java)
                            if (dataClassItem != null) {
                                dataClassItem.favorite = true
                                reference.child(itemId).setValue(dataClassItem)
                                    .addOnSuccessListener {
                                        Log.d("FirebaseSuccess", "Item added to Wishlist successfully")
                                        Toast.makeText(this@DetailActivity, "Barang ditambahkan ke Wishlist", Toast.LENGTH_SHORT).show()
                                        updateFirebaseFavoriteStatus(itemId, isFavorite)
                                    }
                                    .addOnFailureListener {
                                        Log.e("FirebaseError", "Failed to add item to Wishlist: ${it.message}")
                                    }
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Log.e("FirebaseError", "Failed to fetch item details: ${error.message}")
                    }
                })
            } else {
                reference.child(itemId).removeValue()
                    .addOnSuccessListener {
                        Log.d("FirebaseSuccess", "Item removed from Wishlist successfully")
                        Toast.makeText(this@DetailActivity, "Barang dihapus dari Wishlist", Toast.LENGTH_SHORT).show()
                        updateFirebaseFavoriteStatus(itemId, isFavorite)
                    }
                    .addOnFailureListener {
                        Log.e("FirebaseError", "Failed to remove item from Wishlist: ${it.message}")
                    }
            }
            saveFavoriteStatus(itemId, isFavorite)
        }
    }

    private fun saveFavoriteStatus(itemId: String, isFavorite: Boolean) {
        val sharedPreferences = getSharedPreferences("wishlist_pref_${firebaseAuth.currentUser?.uid}", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean(itemId, isFavorite)
        editor.apply()
    }

    private fun getFavoriteStatus(itemId: String): Boolean {
        val sharedPreferences = getSharedPreferences("wishlist_pref_${firebaseAuth.currentUser?.uid}", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(itemId, false)
    }

    private fun updateFirebaseFavoriteStatus(itemId: String, isFavorite: Boolean) {
        val reference = FirebaseDatabase.getInstance().reference.child("postingbarang").child(itemId)

        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val dataClassItem = snapshot.getValue(DataClassItem::class.java)
                    if (dataClassItem != null) {
                        dataClassItem.favorite = isFavorite
                        // Simpan kembali ke Firebase Realtime Database
                        reference.setValue(dataClassItem)
                            .addOnSuccessListener {
                                Log.d("FirebaseSuccess", "Item favorite status updated successfully")
                            }
                            .addOnFailureListener {
                                Log.e("FirebaseError", "Failed to update item favorite status: ${it.message}")
                            }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Failed to fetch item details: ${error.message}")
            }
        })
    }

}
