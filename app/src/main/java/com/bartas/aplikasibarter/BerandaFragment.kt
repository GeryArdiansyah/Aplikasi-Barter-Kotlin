package com.bartas.aplikasibarter

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bartas.aplikasibarter.Models.DataClassItem
import com.bartas.aplikasibarter.databinding.FragmentBerandaBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class BerandaFragment : Fragment() {

    private lateinit var dialog: Dialog
    var databaseReference: DatabaseReference? = null
    var eventListener: ValueEventListener? = null
    private lateinit var dataList: ArrayList<DataClassItem>
    private lateinit var adapter: MyAdapterRekomendasi
    private lateinit var binding: FragmentBerandaBinding
    private lateinit var auth : FirebaseAuth
    private lateinit var uid : String
    private lateinit var username : String
    private var photoUri: Uri? = null
    private val CAMERA_PERMISSION_REQUEST_CODE = 1001

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentBerandaBinding.inflate(inflater, container, false)
        val view = binding.root

        dialog = Dialog(requireContext())

        val username1 = requireActivity().intent.getStringExtra("username")
        binding.tvNamaPengguna.text = username1

        auth = FirebaseAuth.getInstance()
        uid = auth.currentUser?.uid.toString()
        databaseReference = FirebaseDatabase.getInstance().getReference("Pengguna")
        if (uid.isNotEmpty()){
            getUsername()
        }

        binding.searchViewberanda.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                searchItemsByCategory(newText)
                return true
            }
        })

        binding.cardPosting.setOnClickListener {
            showImageSourceDialog()
        }

        // Mengatur onClickListener untuk card elektronik
        binding.cardElektronik.setOnClickListener {
            // Ketika card elektronik diklik, buka ElektronikActivity
            val intent = Intent(requireContext(), ElektronikActivity::class.java)
            startActivity(intent)
        }

        // Mengatur onClickListener untuk card aksesoris
        binding.cardAksesoris.setOnClickListener {
            // Ketika card aksesoris diklik, buka AksesorisActivityy
            val intent = Intent(requireContext(), AksesorisActivity::class.java)
            startActivity(intent)
        }

        // Mengatur onClickListener untuk card perabotan
        binding.cardPerabotan.setOnClickListener {
            // Ketika card perabotan diklik, buka PerabotanActivity
            val intent = Intent(requireContext(), PerabotanActivity::class.java)
            startActivity(intent)
        }

        // Mengatur onClickListener untuk card fashion
        binding.cardFashion.setOnClickListener {
            // Ketika card fashion diklik, buka FashionActivity
            val intent = Intent(requireContext(), FashionActivity::class.java)
            startActivity(intent)
        }

        // Mengatur onClickListener untuk button lihat semua
        binding.btnLihatSemua.setOnClickListener {
            // Ketika button lihat semua diklik, buka RekomendasiActivity
            val intent = Intent(requireContext(), RekomendasiActivity::class.java)
            startActivity(intent)
        }

        // Initialize RecyclerView and Adapter
        binding.recyclerViewRekomendasi.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        dataList = ArrayList()
        adapter = MyAdapterRekomendasi(requireActivity(), dataList)
        binding.recyclerViewRekomendasi.adapter = adapter

        // Initialize Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference("lowrekomen")

        // Show progress dialog
        val builder = AlertDialog.Builder(requireActivity())
        builder.setCancelable(false)
        builder.setView(R.layout.progress_layout)
        val progressDialog = builder.create()
        progressDialog.show()

        // Fetch data from Firebase
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
                progressDialog.dismiss()
            }

            override fun onCancelled(error: DatabaseError) {
                progressDialog.dismiss()
            }
        })

        return view
    }

    private fun getUsername() {
        databaseReference?.child(uid)?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                username = snapshot.child("username").value.toString()
                binding.tvNamaPengguna.text = username
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("PostingBarangActivity", "Error saat mengambil data username dari Firebase: ${error.message}")
            }
        })
    }

    private fun showImageSourceDialog() {
        dialog.setContentView(R.layout.popup_post)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
        dialog.setCancelable(false)

        val btnAmbilFoto = dialog.findViewById<AppCompatButton>(R.id.btn_ambil_foto)
        btnAmbilFoto.setOnClickListener {
            if (checkCameraPermission()) {
                launchCamera()
            } else {
                requestCameraPermission()
            }
        }

        dialog.findViewById<AppCompatButton>(R.id.btn_upload_gallery).setOnClickListener {
            val galleryIntent = Intent(Intent.ACTION_PICK)
            galleryIntent.type = "image/*"
            startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST)
        }

        dialog.findViewById<ImageButton>(R.id.btn_close).setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        databaseReference?.removeEventListener(eventListener!!)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST) {
            if (resultCode == AppCompatActivity.RESULT_OK) {
                if (data != null) {
                    // Handle the case when the image is selected from the gallery
                    val selectedImageUri = data.data
                    val postingIntent = Intent(requireContext(), PostingBarangActivity::class.java)
                    postingIntent.putExtra("selectedImageUri", selectedImageUri.toString())
                    startActivity(postingIntent)
                } else {
                    // Handle the case when the image is captured from the camera
                    val postingIntent = Intent(requireContext(), PostingBarangActivity::class.java)
                    postingIntent.putExtra("selectedImageUri", photoUri.toString())
                    startActivity(postingIntent)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Camera permission is required to capture images.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun launchCamera() {
        val photoFile: File? = try {
            createImageFile()
        } catch (ex: IOException) {
            ex.printStackTrace()
            null
        }

        photoFile?.also {
            val photoURI: Uri = FileProvider.getUriForFile(
                requireContext(),
                "com.bartas.aplikasibarter.fileprovider",
                it
            )
            photoUri = photoURI

            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            startActivityForResult(cameraIntent, PICK_IMAGE_REQUEST)
        }
    }

    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        )
    }

    companion object {
        const val PICK_IMAGE_REQUEST = 1
    }

    private fun searchItemsByCategory(query: String) {
        val filteredList = dataList.filter { item ->
            // Menggunakan fungsi filter untuk menyaring berdasarkan kategori
            item.kategori?.lowercase()?.contains(query.lowercase()) == true
        }.toMutableList()

        adapter.searchDataList(filteredList)
    }
}
