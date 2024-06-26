package com.bartas.aplikasibarter

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bartas.aplikasibarter.Models.DataClassPengguna
import com.google.firebase.auth.FirebaseAuth
import com.bartas.aplikasibarter.databinding.ActivityRegisterBinding
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        binding.btnRegister.setOnClickListener {
            val username = binding.username.text.toString().trim()
            val fullName = binding.nama.text.toString().trim()
            val email = binding.email.text.toString().trim()
            val password = binding.pass.text.toString()
            val konfirmPass = binding.konfirmasipass.text.toString()

            // Show progress dialog
            val builder = AlertDialog.Builder(this@RegisterActivity)
            builder.setCancelable(false)
            builder.setView(R.layout.progress_layout)
            val progressDialog = builder.create()
            progressDialog.show()

            // Validasi input
            if (username.isEmpty() || fullName.isEmpty() || email.isEmpty() || password.isEmpty() || konfirmPass.isEmpty()) {
                if (username.isEmpty()){
                    binding.username.error = "Username tidak boleh kosong"
                }
                if (fullName.isEmpty()){
                    binding.nama.error = "Nama Lengkap tidak boleh kosong"
                }
                if (email.isEmpty()){
                    binding.email.error = "Email tidak boleh kosong"
                }
                if (password.isEmpty()){
                    binding.pass.error = "Password tidak boleh kosong"
                }
                if (konfirmPass.isEmpty()){
                    binding.konfirmasipass.error = "Konfirmasi Password tidak boleh kosong"
                }
                Toast.makeText(this@RegisterActivity, "Mohon isi semua data", Toast.LENGTH_SHORT).show()
                progressDialog.dismiss()
            }else if(!email.matches(emailPattern.toRegex())){
                binding.email.error = "Email tidak valid"
                binding.email.requestFocus()
                Toast.makeText(this@RegisterActivity, "Email tidak valid", Toast.LENGTH_SHORT).show()
                progressDialog.dismiss()
            }else if(password.length < 8){
                binding.pass.error = "Password minimal 8 karakter"
                binding.pass.requestFocus()
                Toast.makeText(this@RegisterActivity, "Password minimal 8 karakter", Toast.LENGTH_SHORT).show()
                progressDialog.dismiss()
            }else if(password != konfirmPass){
                binding.konfirmasipass.error = "Password tidak sama"
                binding.konfirmasipass.requestFocus()
                Toast.makeText(this@RegisterActivity, "Password tidak sama", Toast.LENGTH_SHORT).show()
                progressDialog.dismiss()
            }else{
                firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener{
                    if(it.isSuccessful){
                        val databaseRef = database.reference.child("Pengguna").child(firebaseAuth.currentUser!!.uid)
                        val pengguna : DataClassPengguna = DataClassPengguna(username, fullName, email, firebaseAuth.currentUser!!.uid)
                        databaseRef.setValue(pengguna).addOnCompleteListener{
                            if(it.isSuccessful){
                                Toast.makeText(this@RegisterActivity, "Berhasil Register", Toast.LENGTH_SHORT).show()
                                progressDialog.dismiss()
                                val intent = Intent(this, LoginActivity::class.java)
                                startActivity(intent)
                            }else{
                                Toast.makeText(this@RegisterActivity, "Gagal Register", Toast.LENGTH_SHORT).show()
                                progressDialog.dismiss()
                            }
                        }
                    }else{
                        Toast.makeText(this@RegisterActivity, "Gagal Register", Toast.LENGTH_SHORT).show()
                        progressDialog.dismiss()
                    }
                }
            }
        }

        binding.btnLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }
}
