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
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.bartas.aplikasibarter.Models.DataClassItem
import com.bartas.aplikasibarter.databinding.ActivityUbahBarangBinding
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UbahBarangActivity : AppCompatActivity() {

    private lateinit var imageViewUbah: ImageView
    private lateinit var databaseReference: DatabaseReference
    private lateinit var binding: ActivityUbahBarangBinding
    private lateinit var postinganId: String
    private lateinit var toolbar: Toolbar
    private var photoUri: Uri? = null
    private val CAMERA_PERMISSION_REQUEST_CODE = 1001
    private lateinit var currentPhotoPath: String
    private lateinit var auth : FirebaseAuth
    private lateinit var uid : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUbahBarangBinding.inflate(layoutInflater)
        setContentView(binding.root)

        toolbar = findViewById<Toolbar>(R.id.toolbarBack)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        auth = FirebaseAuth.getInstance()
        uid = auth.currentUser?.uid.toString()

        imageViewUbah = findViewById(R.id.imageViewubah)

        binding.btnubahgambar.setOnClickListener {
            showImageSourceDialog()
        }

        val spinnerKategori = findViewById<Spinner>(R.id.spinnerkategori)
        val item = ArrayAdapter.createFromResource(
            this,
            R.array.kategori_barang,
            android.R.layout.simple_spinner_item
        )
        item.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerKategori.adapter = item

        item.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerKategori.adapter = object : ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_dropdown_item, resources.getStringArray(R.array.kategori_barang)) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                view.typeface = ResourcesCompat.getFont(this@UbahBarangActivity, R.font.poppins_semibold)
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                view.typeface = ResourcesCompat.getFont(this@UbahBarangActivity, R.font.poppins_semibold)
                return view
            }
        }

        // Inisialisasi database reference
        databaseReference = FirebaseDatabase.getInstance().getReference("postingbarang")

        // Mendapatkan postinganId dari intent
        postinganId = intent.getStringExtra("POSTINGAN_ID") ?: ""

        // Mendapatkan referensi ke setiap elemen UI
        val etNamaBarang: EditText = findViewById(R.id.namabarang)
        val etKategori: Spinner = findViewById(R.id.spinnerkategori)
        val etHarga: EditText = findViewById(R.id.hargabarang)
        val etDeskripsi: EditText = findViewById(R.id.deskripsi)
        val etLokasi: EditText = findViewById(R.id.lokasi)
        val imgBarang: ImageView = findViewById(R.id.imageViewubah)
        val btnUbahBarang: MaterialButton = findViewById(R.id.btnubahbarang)

        // Menampilkan data dari Firebase ke dalam EditText, Spinner, dan ImageView
        databaseReference.child(postinganId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val postingan = snapshot.getValue(DataClassItem::class.java)
                    postingan?.let {
                        etNamaBarang.setText(it.nama ?: "")
                        etKategori.setSelection(getIndexSpinner(etKategori, it.kategori))
                        etHarga.setText(it.harga ?: "")
                        etDeskripsi.setText(it.deskripsi ?: "")
                        etLokasi.setText(it.lokasi ?: "")

                        // Tampilkan gambar menggunakan Glide
                        Glide.with(this@UbahBarangActivity)
                            .load(it.imageUrl)
                            .into(imgBarang)
                    }
                } else {
                    Toast.makeText(this@UbahBarangActivity, "Data tidak ditemukan", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@UbahBarangActivity, "Gagal mengambil data", Toast.LENGTH_SHORT).show()
            }
        })
        btnUbahBarang.setOnClickListener {
            val namaBarang = etNamaBarang.text.toString().trim()
            val kategori = etKategori.selectedItem.toString().trim()
            val hargaBarang = etHarga.text.toString().trim()
            val deskripsi = etDeskripsi.text.toString().trim()
            val lokasi = etLokasi.text.toString().trim()
            val imageUrl = photoUri?.toString() ?: ""

            val updateData = HashMap<String, Any>()
            updateData["imageUrl"] = imageUrl
            updateData["nama"] = namaBarang
            updateData["kategori"] = kategori
            updateData["harga"] = hargaBarang
            updateData["deskripsi"] = deskripsi
            updateData["lokasi"] = lokasi

            val builder = AlertDialog.Builder(this@UbahBarangActivity)
            builder.setCancelable(false)
            builder.setView(R.layout.progress_layout)
            val progressDialog = builder.create()
            progressDialog.show()
            databaseReference.child(postinganId).updateChildren(updateData)
                .addOnSuccessListener {
                    progressDialog.dismiss()
                    Toast.makeText(this@UbahBarangActivity, "Barang berhasil diubah", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    progressDialog.dismiss()
                    Toast.makeText(this@UbahBarangActivity, "Gagal mengubah barang", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun getIndexSpinner(spinner: Spinner, myString: String?): Int {
        myString?.let { str ->
            for (i in 0 until spinner.count) {
                if (spinner.getItemAtPosition(i).toString().equals(str, ignoreCase = true)) {
                    return i
                }
            }
        }
        return 0
    }

    private fun showImageSourceDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.popup_post)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
        dialog.setCancelable(false)

        val btnAmbilFoto = dialog.findViewById<AppCompatButton>(R.id.btn_ambil_foto)
        btnAmbilFoto.setOnClickListener {
            if (checkCameraPermission()) {
                launchCamera()
                dialog.dismiss()
            } else {
                requestCameraPermission()
            }
        }

        dialog.findViewById<AppCompatButton>(R.id.btn_upload_gallery).setOnClickListener {
            val galleryIntent = Intent(Intent.ACTION_PICK)
            galleryIntent.type = "image/*"
            startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST)
            dialog.dismiss()
        }

        dialog.findViewById<ImageButton>(R.id.btn_close).setOnClickListener {
            dialog.dismiss()
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
                this@UbahBarangActivity,
                "com.bartas.aplikasibarter.fileprovider",
                it
            )
            currentPhotoPath = it.absolutePath // Simpan path file foto yang diambil dari kamera
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            startActivityForResult(cameraIntent, PICK_IMAGE_REQUEST)

            Glide.with(this)
                .load(photoURI) // Gunakan photoURI yang telah dibuat sebelumnya
                .into(imageViewUbah)
        }
    }

    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = this@UbahBarangActivity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        )
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == AppCompatActivity.RESULT_OK) {
            if (data != null && data.data != null) {
                val selectedImageUri = data.data
                imageViewUbah.setImageURI(selectedImageUri)
                photoUri = selectedImageUri
                uploadImageToFirebaseStorage(selectedImageUri!!)
            } else {
                val file = File(currentPhotoPath)
                photoUri = FileProvider.getUriForFile(
                    this@UbahBarangActivity,
                    "com.bartas.aplikasibarter.fileprovider",
                    file
                )
                imageViewUbah.setImageURI(photoUri)
                uploadImageToFirebaseStorage(photoUri!!)
            }
        }
    }


    private fun uploadImageToFirebaseStorage(uri: Uri) {
        val storageReference = FirebaseStorage.getInstance().reference
        val imageRef = storageReference.child("images/$uid/${System.currentTimeMillis()}.jpg")

        val builder = AlertDialog.Builder(this@UbahBarangActivity)
        builder.setCancelable(false)
        builder.setView(R.layout.progress_layout)
        val progressDialog = builder.create()
        progressDialog.show()
        imageRef.putFile(uri)
            .addOnSuccessListener { taskSnapshot ->
                progressDialog.dismiss()
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    val imageUrl = downloadUri.toString()
                    databaseReference.child(postinganId).child("imageUrl").setValue(imageUrl)
                        .addOnSuccessListener {
                        }
                        .addOnFailureListener {
                            Toast.makeText(this@UbahBarangActivity, "Gagal mengunggah gambar ke Realtime Database", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { exception ->
                // Jika gagal diunggah ke Firebase Storage, tampilkan pesan error
                Toast.makeText(this@UbahBarangActivity, "Gagal mengunggah gambar ke Firebase Storage: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }



    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this@UbahBarangActivity,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this@UbahBarangActivity,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera()
            } else {
                Toast.makeText(
                    this@UbahBarangActivity,
                    "Camera permission is required to capture images.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    companion object {
        const val PICK_IMAGE_REQUEST = 1
    }
}