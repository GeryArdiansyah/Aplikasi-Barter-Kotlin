package com.bartas.aplikasibarter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bartas.aplikasibarter.Adapter.BarangTerjualAdapter
import com.bartas.aplikasibarter.Models.DataClassItem
import com.google.firebase.database.*

class BarangTerjualActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var soldBarangAdapter: BarangTerjualAdapter
    private lateinit var databaseReference: DatabaseReference
    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barang_terjual)

        toolbar = findViewById<Toolbar>(R.id.toolbarBack)

        toolbar.setNavigationOnClickListener {

            finish()
        }

        // Inisialisasi RecyclerView
        recyclerView = findViewById(R.id.riwayatRecyclerView)
        soldBarangAdapter = BarangTerjualAdapter(listOf())
        recyclerView.adapter = soldBarangAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Inisialisasi database Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference("soldout")

        // Mendapatkan data barang yang sudah terjual dari Firebase Database
        retrieveSoldBarangFromDatabase()
    }

    // Metode untuk mendapatkan daftar barang yang sudah terjual dari Firebase Database
    private fun retrieveSoldBarangFromDatabase() {
        val soldBarangList = mutableListOf<DataClassItem>()

        // Ambil data dari Firebase yang sudah dijual
        val databaseRef = FirebaseDatabase.getInstance().getReference("soldout")
        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (dataSnapshot in snapshot.children) {
                    val soldBarang = dataSnapshot.getValue(DataClassItem::class.java)
                    soldBarang?.let {
                        soldBarangList.add(it)
                    }
                }
                // Setelah mendapatkan daftar barang yang sudah terjual, update adapter RecyclerView
                soldBarangAdapter.updateData(soldBarangList)
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }
}



