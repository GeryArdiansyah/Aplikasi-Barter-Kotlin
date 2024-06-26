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
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.bartas.aplikasibarter.databinding.ActivityPostingBarangBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PostingBarangActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPostingBarangBinding
    private lateinit var imageViewPosting: ImageView
    private lateinit var auth : FirebaseAuth
    private lateinit var uid : String
    private lateinit var username : String
    private lateinit var databaseReference: DatabaseReference
    private lateinit var storage: FirebaseStorage
    private lateinit var storageReference: StorageReference
    private var photoUri: Uri? = null
    private val CAMERA_PERMISSION_REQUEST_CODE = 1001
    private val MAPS_ACTIVITY_REQUEST_CODE = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostingBarangBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
                view.typeface = ResourcesCompat.getFont(this@PostingBarangActivity, R.font.poppins_semibold)
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                view.typeface = ResourcesCompat.getFont(this@PostingBarangActivity, R.font.poppins_semibold)
                return view
            }
        }

        imageViewPosting = findViewById(R.id.imageViewPosting)
        storage = FirebaseStorage.getInstance()
        storageReference = storage.reference

        val selectedImageUri = intent.getStringExtra("selectedImageUri")

        binding.imageViewPosting.setImageURI(Uri.parse(selectedImageUri))

        binding.changeImageButton.setOnClickListener {
            showImageSourceDialog()
        }

        auth = FirebaseAuth.getInstance()
        uid = auth.currentUser?.uid.toString()
        databaseReference = FirebaseDatabase.getInstance().getReference("Pengguna")
        if (uid.isNotEmpty()){
            getUsername()
        }

        binding.locationIcon.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            startActivityForResult(intent, MAPS_ACTIVITY_REQUEST_CODE)
        }

        val btnPajang = findViewById<Button>(R.id.btnpajangbarang)
        btnPajang.setOnClickListener {
            Log.d("PostingBarangActivity", "Tombol Pajang Barang ditekan")
            postBarangToFirebase(Uri.parse(selectedImageUri))
        }

    }

    private fun getUsername() {
        databaseReference.child(uid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                username = snapshot.child("username").value.toString()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("PostingBarangActivity", "Error saat mengambil data username dari Firebase: ${error.message}")
            }
        })
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

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this@PostingBarangActivity,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this@PostingBarangActivity,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == MAPS_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {
            val selectedLocation = data?.getStringExtra("selectedLocation")
            binding.lokasi.setText(selectedLocation)
            Log.d("PostingBarangActivity", "Lokasi barang: $selectedLocation")
        }

        if (requestCode == PostingBarangActivity.PICK_IMAGE_REQUEST && resultCode == AppCompatActivity.RESULT_OK) {
            if (data != null && data.data != null) {
                val selectedImageUri = data.data
                imageViewPosting.setImageURI(selectedImageUri)
                photoUri = selectedImageUri
            } else {
                imageViewPosting.setImageURI(photoUri)
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
                    this@PostingBarangActivity,
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
                this@PostingBarangActivity,
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
        val storageDir: File? = this@PostingBarangActivity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        )
    }

    companion object {
        const val PICK_IMAGE_REQUEST = 1
    }

    private fun postBarangToFirebase(imageUri: Uri?) {
        val namaBarang = binding.namaBarang.text.toString()
        val hargaBarang = binding.hargaBarang.text.toString()
        val descBarang = binding.deskripsiBarang.text.toString()
        val lokasiBarang = binding.lokasi.text.toString()
        val kategoriBarang = binding.spinnerkategori.selectedItem.toString()
        val imageUrl = imageUri?.toString() ?: ""

        if (imageUrl.isEmpty()) {
            showValidationWarning("Mohon masukan foto barang.")
            return
        }

        if (namaBarang.isEmpty()) {
            showValidationWarning("Mohon isi nama barang.")
            return
        }

        if (kategoriBarang == "Pilih Kategori") {
            showValidationWarning("Mohon pilih kategori yang sesuai.")
            return
        }

        if (hargaBarang.isEmpty()) {
            showValidationWarning("Mohon isi harga barang.")
            return
        }

        if (descBarang.isEmpty()) {
            showValidationWarning("Mohon isi deskripsi barang.")
            return
        }

        if (lokasiBarang.isEmpty()) {
            showValidationWarning("Mohon isi lokasi barang.")
            return
        }

        val imageFileName = "images/$uid/${System.currentTimeMillis()}.jpg"
        val imageRef = storageReference.child(imageFileName)

        val builder = AlertDialog.Builder(this@PostingBarangActivity)
        builder.setCancelable(false)
        builder.setView(R.layout.progress_layout)
        val progressDialog = builder.create()
        progressDialog.show()
        imageUri?.let { uri ->
            imageRef.putFile(uri)
                .addOnSuccessListener { taskSnapshot ->
                    progressDialog.dismiss()
                    imageRef.downloadUrl.addOnSuccessListener { imageUrl ->
                        val imageUrlString = imageUrl.toString()

                        val database = FirebaseDatabase.getInstance()
                        val reference = database.getReference("postingbarang")

                        val barang = HashMap<String, Any>()
                        barang["uid"] = uid
                        barang["username"] = username
                        barang["nama"] = namaBarang
                        barang["harga"] = hargaBarang
                        barang["deskripsi"] = descBarang
                        barang["lokasi"] = lokasiBarang
                        barang["kategori"] = kategoriBarang
                        barang["imageUrl"] = imageUrlString // Simpan URL gambar ke dalam data barang

                        val key = reference.push().key ?: ""
                        barang["id"] = key

                        reference.child(key).setValue(barang)
                            .addOnSuccessListener {
                                showSuccessPopupSuccess()
                            }
                            .addOnFailureListener { exception ->
                                showErrorLog(exception.message ?: "Gagal menyimpan data barang ke Firebase")
                            }
                    }
                }
                .addOnFailureListener { exception ->
                    showErrorLog("Failed to upload image to Firebase Storage: ${exception.message}")
                }
        }
    }


    private fun showErrorLog(errorMessage: String) {
        Log.e("PostingBarangActivity", "Gagal menyimpan data barang ke Firebase: $errorMessage")
    }
    private fun showSuccessPopupSuccess() {
        val intent = Intent(this, SuksesPopupActivity::class.java)
        startActivity(intent)
    }

    private fun showValidationWarning(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}