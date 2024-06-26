package com.bartas.aplikasibarter

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import com.bartas.aplikasibarter.Adapter.MyAdapterKategori
import com.bartas.aplikasibarter.Models.DataClassItem
import com.bartas.aplikasibarter.databinding.ActivitySearchFashionBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*

class FashionActivity : AppCompatActivity() {

    var databaseReference: DatabaseReference? = null
    var eventListener: ValueEventListener? = null
    private lateinit var toolbar: Toolbar
    private lateinit var dataList: ArrayList<DataClassItem>
    private lateinit var adapter: MyAdapterKategori
    private lateinit var binding: ActivitySearchFashionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchFashionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val user = FirebaseAuth.getInstance().currentUser
        val userId = user?.uid

        toolbar = findViewById<Toolbar>(R.id.toolbarBack)

        toolbar.setNavigationOnClickListener {

            finish()
        }

        val gridLayoutManager = GridLayoutManager(this@FashionActivity, 2)
        binding.recyclerView.layoutManager = gridLayoutManager
        binding.searchView.clearFocus()
        val builder = AlertDialog.Builder(this@FashionActivity)
        builder.setCancelable(false)

        builder.setView(R.layout.progress_layout)
        val dialog = builder.create()
        dialog.show()
        dataList = ArrayList()
        adapter = MyAdapterKategori(this@FashionActivity, dataList, databaseReference)
        binding.recyclerView.adapter = adapter
        databaseReference = FirebaseDatabase.getInstance().getReference("postingbarang")
        dialog.show()
        eventListener = databaseReference!!.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                dataList.clear()
                for (itemSnapshot in snapshot.children) {
                    val dataClass = itemSnapshot.getValue(DataClassItem::class.java)
                    if (dataClass?.kategori == "Fashion") {
                        dataClass.userId = userId
                        dataList.add(dataClass)
                    }
                }
                adapter.notifyDataSetChanged()
                dialog.dismiss()
                val sharedPreferences: SharedPreferences = getSharedPreferences("wishlist_pref_${userId}", Context.MODE_PRIVATE)
                for (dataClass in dataList) {
                    val isChecked = sharedPreferences.getBoolean(dataClass.id, false)
                    dataClass.favorite = isChecked
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                dialog.dismiss()
            }
        })

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }
            override fun onQueryTextChange(newText: String): Boolean {
                searchList(newText)
                return true
            }
        })
    }

    fun searchList(text: String) {
        val searchList = java.util.ArrayList<DataClassItem>()
        for (dataClass in dataList) {
            if (dataClass.nama?.lowercase()
                    ?.contains(text.lowercase(Locale.getDefault())) == true
            ) {
                searchList.add(dataClass)
            }
        }
        adapter.searchDataList(searchList)
    }
}