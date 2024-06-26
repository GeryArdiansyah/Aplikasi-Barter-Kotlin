package com.bartas.aplikasibarter

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bartas.aplikasibarter.Models.DataClassPosting
import com.bartas.aplikasibarter.databinding.ActivityPostinganSayaBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PostinganSayaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostinganSayaBinding
    private lateinit var postinganAdapter: PostinganSayaAdapter
    private lateinit var toolbar: Toolbar
    private lateinit var auth : FirebaseAuth
    private var photoUri: Uri? = null
    private val CAMERA_PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostinganSayaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        toolbar = findViewById<Toolbar>(R.id.toolbarBack)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        // Inisialisasi RecyclerView
        binding.recyclerViewPostingan.layoutManager = LinearLayoutManager(this)
        postinganAdapter = PostinganSayaAdapter()
        binding.recyclerViewPostingan.adapter = postinganAdapter
        // Load data postingan dari Firebase
        loadDataPostingan()

        binding.btnpajang.setOnClickListener {
            showImageSourceDialog()
        }
    }

    private fun loadDataPostingan() {
        val uid = auth.currentUser?.uid.toString()
        val databaseReference = FirebaseDatabase.getInstance().getReference("postingbarang")

        databaseReference.orderByChild("uid").equalTo(uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val postinganList = mutableListOf<DataClassPosting>()
                    for (postSnapshot in snapshot.children) {
                        val postingan = postSnapshot.getValue(DataClassPosting::class.java)
                        postingan?.let { postinganList.add(it) }
                    }
                    postinganAdapter.submitList(postinganList)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                    Log.e("PostinganSayaActivity", "Error saat mengambil data postingan dari Firebase: ${error.message}")
                }
            })
    }

    private fun showImageSourceDialog() {
        val dialog1 = Dialog(this@PostinganSayaActivity)
        dialog1.setContentView(R.layout.popup_post)
        dialog1.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog1.show()
        dialog1.setCancelable(false)

        val btnAmbilFoto = dialog1.findViewById<AppCompatButton>(R.id.btn_ambil_foto)
        btnAmbilFoto.setOnClickListener {
            if (checkCameraPermission()) {
                launchCamera()
            } else {
                requestCameraPermission()
            }
        }

        dialog1.findViewById<AppCompatButton>(R.id.btn_upload_gallery).setOnClickListener {
            val galleryIntent = Intent(Intent.ACTION_PICK)
            galleryIntent.type = "image/*"
            startActivityForResult(galleryIntent, PostinganSayaActivity.PICK_IMAGE_REQUEST)
        }

        dialog1.findViewById<ImageButton>(R.id.btn_close).setOnClickListener {
            dialog1.dismiss()
        }
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this@PostinganSayaActivity,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this@PostinganSayaActivity,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST) {
            if (resultCode == AppCompatActivity.RESULT_OK) {
                if (data != null) {
                    // Handle the case when the image is selected from the gallery
                    val selectedImageUri = data.data
                    val postingIntent = Intent(this@PostinganSayaActivity, PostingBarangActivity::class.java)
                    postingIntent.putExtra("selectedImageUri", selectedImageUri.toString())
                    startActivity(postingIntent)
                } else {
                    // Handle the case when the image is captured from the camera
                    val postingIntent = Intent(this@PostinganSayaActivity, PostingBarangActivity::class.java)
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
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera()
            } else {
                Toast.makeText(
                    this@PostinganSayaActivity,
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
                this@PostinganSayaActivity,
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
        val storageDir: File? = this@PostinganSayaActivity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        )
    }

    companion object {
        const val PICK_IMAGE_REQUEST = 1
    }
}