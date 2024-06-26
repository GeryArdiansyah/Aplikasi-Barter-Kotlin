package com.bartas.aplikasibarter

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bartas.aplikasibarter.databinding.ActivityEditProfilBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat

import java.util.*

class EditProfilActivity : AppCompatActivity() {

    private lateinit var imageViewFotoProfil: ImageView
    private lateinit var binding: ActivityEditProfilBinding
    private lateinit var editTextFirstName: EditText
    private lateinit var editTextLastName: EditText
    private lateinit var editTextTanggalLahir: EditText
    private lateinit var editTextNomor: EditText
    private lateinit var editTextAlamat: EditText
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference
    private lateinit var dialog: Dialog
    private lateinit var successDialog: Dialog
    private lateinit var auth : FirebaseAuth
    private lateinit var uid : String
    private lateinit var username1 : String
    private lateinit var radioGroupGender: RadioGroup
    private lateinit var radioLakiLaki: RadioButton
    private lateinit var radioPerempuan: RadioButton
    private var photoUri: Uri? = null
    private val CAMERA_PERMISSION_REQUEST_CODE = 1001
    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val builder = AlertDialog.Builder(this@EditProfilActivity)
        builder.setCancelable(false)
        builder.setView(R.layout.progress_layout)
        val progressDialog = builder.create()
        progressDialog.show()

        binding = ActivityEditProfilBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        toolbar = findViewById<Toolbar>(R.id.toolbarBack)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        auth = FirebaseAuth.getInstance()
        uid = auth.currentUser?.uid.toString()
        databaseReference = FirebaseDatabase.getInstance().getReference("Pengguna")
        if (uid.isNotEmpty()){
            getUsername()
        }

        val btnEditFoto = findViewById<ImageButton>(R.id.btnEditFoto)
        btnEditFoto.setOnClickListener {
            showImageSourceDialog()
        }

        // Inisialisasi views
        imageViewFotoProfil = findViewById(R.id.imageViewFotoProfil)
        editTextFirstName = findViewById(R.id.editTextFirstName)
        editTextLastName = findViewById(R.id.editTextLastName)
        editTextTanggalLahir = findViewById(R.id.editTextTanggalLahir)
        editTextNomor = findViewById(R.id.editTextNomor)
        editTextAlamat = findViewById(R.id.editTextAlamat)
        radioGroupGender = findViewById(R.id.radioGroupGender)
        radioLakiLaki = findViewById(R.id.radioLakiLaki)
        radioPerempuan = findViewById(R.id.radioPerempuan)

        firebaseAuth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().reference.child("Pengguna")

        loadUserData()
        editTextTanggalLahir.setOnClickListener {
            showDatePickerDialog()
        }

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val username = user.displayName
            val email = user.email

            binding.usernm.text = username
            binding.emailaddress.text = email
        }

        val btnSimpan = findViewById<Button>(R.id.btnSimpan)
        btnSimpan.setOnClickListener {
            showConfirmationDialog()
        }
        progressDialog.dismiss()
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Kamera", "Gallery")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Pilih Sumber Gambar")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> openCamera()
                1 -> openGallery()
            }
        }
        builder.show()
    }

    private fun openCamera() {
        if (checkCameraPermission()) {
            launchCamera()
        } else {
            requestCameraPermission()
        }
    }

    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK)
        galleryIntent.type = "image/*"
        startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST)
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this@EditProfilActivity,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this@EditProfilActivity,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == AppCompatActivity.RESULT_OK) {
            if (data != null && data.data != null) {
                val selectedImageUri = data.data
                imageViewFotoProfil.setImageURI(selectedImageUri)
                photoUri = selectedImageUri
            } else {
                imageViewFotoProfil.setImageURI(photoUri)
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
                    this@EditProfilActivity,
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
                this@EditProfilActivity,
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
        val storageDir: File? = this@EditProfilActivity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        )
    }

    companion object {
        const val PICK_IMAGE_REQUEST = 1
    }

    private fun getUsername() {
        databaseReference?.child(uid)?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                username1 = snapshot.child("username").value.toString()
                binding.usernm.text = username1
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("PostingBarangActivity", "Error saat mengambil data username dari Firebase: ${error.message}")
            }
        })
    }

    private fun loadUserData() {
        val currentUserID = firebaseAuth.currentUser?.uid
        currentUserID?.let {
            databaseReference.child(currentUserID).get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val photoUrl = snapshot.child("photo_url").value?.toString() ?: "@drawable/ic_person"
                    val firstName = snapshot.child("nama_depan").value?.toString() ?: ""
                    val lastName = snapshot.child("nama_belakang").value?.toString() ?: ""
                    val tanggalLahir = snapshot.child("tanggal_lahir").value?.toString() ?: ""
                    val nomor = snapshot.child("nomor_telepon").value?.toString() ?: ""
                    val alamat = snapshot.child("alamat").value?.toString() ?: ""
                    val gender = snapshot.child("gender").value?.toString() ?: ""
                    if (gender.isNotEmpty()) {
                        if (gender == "Pria") {
                            radioLakiLaki.isChecked = true
                        } else if (gender == "Wanita") {
                            radioPerempuan.isChecked = true
                        }
                    }

                    editTextFirstName.setText(firstName)
                    editTextLastName.setText(lastName)
                    editTextTanggalLahir.setText(tanggalLahir)
                    editTextNomor.setText(nomor)
                    editTextAlamat.setText(alamat)
                    if (photoUrl.startsWith("http")) {
                        Glide.with(this@EditProfilActivity)
                            .load(photoUrl)
                            .apply(RequestOptions().placeholder(R.drawable.foto_profil)) // Placeholder image
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(imageViewFotoProfil)
                    } else {
                        imageViewFotoProfil.setImageResource(R.drawable.foto_profil)
                    }
                }
            }.addOnFailureListener {
            }
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = "$dayOfMonth/${month + 1}/$year"
                editTextTanggalLahir.setText(selectedDate)
            },
            year,
            month,
            dayOfMonth
        )

        datePicker.datePicker.maxDate = System.currentTimeMillis()
        datePicker.show()
    }

    private fun showConfirmationDialog() {
        dialog = Dialog(this)
        dialog.setContentView(R.layout.popup_ubah_profil)
        dialog.setCancelable(false)

        val btnYes = dialog.findViewById<Button>(R.id.btn_yes)
        val btnNo = dialog.findViewById<Button>(R.id.btn_no)

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnYes.setOnClickListener {
            updateProfile()
            dialog.dismiss()
        }

        btnNo.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateProfile() {
        val builder = AlertDialog.Builder(this@EditProfilActivity)
        builder.setCancelable(false)
        builder.setView(R.layout.progress_layout)
        val progressDialog = builder.create()
        progressDialog.show()
        val currentUser = firebaseAuth.currentUser
        currentUser?.let {
            val newFirstName = editTextFirstName.text.toString().trim()
            val newLastName = editTextLastName.text.toString().trim()
            val newTanggalLahir = editTextTanggalLahir.text.toString().trim()
            val newNomor = editTextNomor.text.toString().trim()
            val newAlamat = editTextAlamat.text.toString().trim()
            val newGender = when (radioGroupGender.checkedRadioButtonId) {
                R.id.radioLakiLaki -> "Pria"
                R.id.radioPerempuan -> "Wanita"
                else -> ""
            }

            // Update profile data
            val userUpdates = HashMap<String, Any>()
            userUpdates["nama_depan"] = newFirstName
            userUpdates["nama_belakang"] = newLastName
            userUpdates["tanggal_lahir"] = newTanggalLahir
            userUpdates["nomor_telepon"] = newNomor
            userUpdates["alamat"] = newAlamat
            userUpdates["gender"] = newGender

            if (photoUri != null) {
                val storageRef = FirebaseStorage.getInstance().reference
                val photoRef = storageRef.child("profile_photos/${currentUser.uid}.jpg")

                val uploadTask = photoRef.putFile(photoUri!!)
                uploadTask.continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let {
                            throw it
                        }
                    }
                    photoRef.downloadUrl
                }.addOnCompleteListener { task ->
                    progressDialog.dismiss()
                    if (task.isSuccessful) {
                        val photoUrl = task.result.toString()
                        userUpdates["photo_url"] = photoUrl
                        databaseReference.child(currentUser.uid).updateChildren(userUpdates)
                            .addOnCompleteListener { updateTask ->
                                progressDialog.dismiss()
                                if (updateTask.isSuccessful) {
                                    showSuccessDialog()
                                    dialog.dismiss()
                                } else {
                                    Toast.makeText(
                                        this@EditProfilActivity,
                                        "Failed to update profile.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    } else {
                        Toast.makeText(
                            this@EditProfilActivity,
                            "Failed to get photo URL.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                databaseReference.child(currentUser.uid).updateChildren(userUpdates)
                    .addOnCompleteListener { task ->
                        progressDialog.dismiss()
                        if (task.isSuccessful) {
                            showSuccessDialog()
                            dialog.dismiss()
                        } else {
                            Toast.makeText(
                                this@EditProfilActivity,
                                "Failed to update profile.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            }
        }
    }

    private fun showSuccessDialog() {
        if (!isFinishing) {
            val sukses = Dialog(this)
            sukses.setContentView(R.layout.popup_sukses_ubah_profil)
            sukses.setCancelable(false)

            val btnOkay = sukses.findViewById<Button>(R.id.btn_okay)

            sukses.window?.setBackgroundDrawableResource(android.R.color.transparent)

            btnOkay.setOnClickListener {
                onBackPressed()
                sukses.dismiss()
            }
            sukses.show()
        }
    }
}
