package com.bartas.aplikasibarter

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import com.bartas.aplikasibarter.Models.DataClassItem
import com.bartas.aplikasibarter.databinding.ActivityLihatRekomendasiBinding
import com.google.firebase.database.*
import java.util.*

class RekomendasiActivity : AppCompatActivity() {

    var databaseReference: DatabaseReference? = null
    var eventListener: ValueEventListener? = null
    private lateinit var toolbar: Toolbar
    private lateinit var dataList: ArrayList<DataClassItem>
    private lateinit var adapter:  MyAdapterRekomendasi
    private lateinit var binding: ActivityLihatRekomendasiBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLihatRekomendasiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        toolbar = findViewById<Toolbar>(R.id.toolbarBack)

        toolbar.setNavigationOnClickListener {
            // Panggil fungsi untuk berpindah ke MainActivity tanpa animasi
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
            startActivity(intent)
        }

        val gridLayoutManager = GridLayoutManager(this@RekomendasiActivity, 2)
        binding.recyclerView.layoutManager = gridLayoutManager
        val builder = AlertDialog.Builder(this@RekomendasiActivity)
        builder.setCancelable(false)

        builder.setView(R.layout.progress_layout)
        val dialog = builder.create()
        dialog.show()

        dataList = ArrayList()
        adapter = MyAdapterRekomendasi(this@RekomendasiActivity, dataList)
        binding.recyclerView.adapter = adapter
        databaseReference = FirebaseDatabase.getInstance().getReference("fullrekomen")
        dialog.show()
        eventListener = databaseReference!!.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                dataList.clear()
                for (itemSnapshot in snapshot.children) {
                    val dataClass = itemSnapshot.getValue(DataClassItem::class.java)
                    if (dataClass != null) {
                        dataList.add(dataClass)
                    }
                }
                adapter.notifyDataSetChanged()
                dialog.dismiss()
            }
            override fun onCancelled(error: DatabaseError) {
                dialog.dismiss()
            }
        })
    }
}