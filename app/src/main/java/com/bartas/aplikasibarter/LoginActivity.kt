package com.bartas.aplikasibarter

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bartas.aplikasibarter.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var mGoogleSign: GoogleSignInClient
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        private const val RC_SIGN_IN = 123
        const val PREFS_NAME = "MyPrefsFile"
        const val KEY_IS_LOGGED_IN = "isLoggedIn"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        binding.btnLogin.setOnClickListener {
            moveToMainActivity()
        }

        binding.btnRegister.setOnClickListener {
            moveToRegisterActivity()
        }

        binding.cardGoogle.setOnClickListener {
            signInWithGoogle()
        }

        if (isLoggedIn()) {
            val username = sharedPreferences.getString("username", "") ?: ""
            moveToMainActivity(username)
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.google_client_id))
            .requestEmail()
            .build()

        mGoogleSign = GoogleSignIn.getClient(this, gso)
    }

    private fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    private fun handleSuccessfulLogin(username: String) {
        saveLoginStatus()
        saveUserData(username)
        moveToMainActivity(username)
    }

    private fun saveLoginStatus() {
        val editor = sharedPreferences.edit()
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.apply()
    }

    private fun saveUserData(username: String) {
        val editor = sharedPreferences.edit()
        editor.putString("username", username)
        editor.apply()
    }

    private fun moveToMainActivity(username: String) {
        val intent = Intent(this@LoginActivity, MainActivity::class.java)
        intent.putExtra("username", username)
        startActivity(intent)
        finish()
    }


    private fun signInWithGoogle() {
        mGoogleSign.signOut().addOnCompleteListener(this) {
        }
        val signInIntent = mGoogleSign.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleGoogleSignInResult(task)
        }
    }

    private fun handleGoogleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            val email = account?.email
            if (email != null) {
                getEmail(email) { userEmail ->
                    if (userEmail.isNotEmpty()) {
                        firebaseAuthWithGoogle(account.idToken!!)
                    } else {
                        Toast.makeText(
                            this@LoginActivity,
                            "Akun Google belum terdaftar",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                Toast.makeText(
                    this@LoginActivity,
                    "Gagal mendapatkan informasi email dari akun Google",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: ApiException) {
            Toast.makeText(this, "Login Gagal", Toast.LENGTH_SHORT).show()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val builder = AlertDialog.Builder(this@LoginActivity)
        builder.setCancelable(false)
        builder.setView(R.layout.progress_layout)
        val progressDialog = builder.create()
        progressDialog.show()
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                progressDialog.dismiss()
                if (task.isSuccessful) {
                    Log.d("LoginActivity", "signInWithCredential:success")
                    val user = firebaseAuth.currentUser
                    Toast.makeText(this, "Login Berhasil", Toast.LENGTH_SHORT).show()

                    val userId = user?.uid ?: ""
                    val databaseReference = FirebaseDatabase.getInstance().getReference("Pengguna")
                    val query = databaseReference.child(userId)

                    query.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (dataSnapshot.exists()) {
                                // Pengguna sudah terdaftar, lanjutkan login
                                val username = dataSnapshot.child("username").getValue(String::class.java)
                                if (!username.isNullOrBlank()) {
                                    handleSuccessfulLogin(username)
                                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                    intent.putExtra("username", username)
                                    startActivity(intent)
                                } else {
                                    Toast.makeText(
                                        this@LoginActivity,
                                        "Login Gagal: Data pengguna tidak ditemukan",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                // Pengguna belum terdaftar, tampilkan pesan
                                Toast.makeText(
                                    this@LoginActivity,
                                    "Akun Google belum terdaftar",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            Toast.makeText(
                                this@LoginActivity,
                                "Error: ${databaseError.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })

                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("LoginActivity", "signInWithCredential:failure", task.exception)
                    Toast.makeText(this, "Login Gagal", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun moveToMainActivity() {
        // Show progress dialog
        val builder = AlertDialog.Builder(this@LoginActivity)
        builder.setCancelable(false)
        builder.setView(R.layout.progress_layout)
        val progressDialog = builder.create()
        progressDialog.show()

        val userCredential = binding.userEmail.text.toString().trim()
        val password = binding.pass.text.toString()

        if (userCredential.isEmpty() || password.isEmpty()) {
            if (userCredential.isEmpty()) {
                binding.userEmail.error = "Username atau Email tidak boleh kosong"
                binding.userEmail.requestFocus()
            }
            if (password.isEmpty()) {
                binding.pass.error = "Password tidak boleh kosong"
                binding.pass.requestFocus()
            }
            progressDialog.dismiss()
            Toast.makeText(this, "Mohon isi semua data", Toast.LENGTH_SHORT).show()
        } else if (password.length < 8) {
            binding.pass.error = "Password minimal 8 karakter"
            binding.pass.requestFocus()
            progressDialog.dismiss()
        }else {
            if (userCredential.contains("@")) {
                signInWithEmail(userCredential, password, progressDialog)
            } else {
                signInWithUsername(userCredential, password, progressDialog)
            }
        }
    }

    private fun signInWithEmail(userCredential: String, password: String, progressDialog: AlertDialog?) {
        getEmail(userCredential){email->
            Log.d("LoginActivity", "Email from database: $email or $userCredential")
            if (email.isNotEmpty()) {
                firebaseAuth.signInWithEmailAndPassword(userCredential, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            progressDialog?.dismiss()
                            Toast.makeText(this, "Login berhasil", Toast.LENGTH_SHORT).show()
                            handleSuccessfulLogin(email)
                            val intent = Intent(this, MainActivity::class.java)
                            intent.putExtra("username", email)
                            startActivity(intent)
                            finish()
                        } else {
                            Log.e("LoginActivity", "Authentication failed: ${task.exception?.message}")
                            progressDialog?.dismiss()
                            Toast.makeText(
                                this,
                                "Email dan Password tidak sesuai",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            } else {
                progressDialog?.dismiss()
                Toast.makeText(
                    this,
                    "Pengguna dengan email $userCredential belum terdaftar",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun getEmail(email: String, callback: (String) -> Unit) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("Pengguna")

        val query = databaseReference.orderByChild("email").equalTo(email)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var Email = ""

                if (dataSnapshot.exists()) {
                    for (userSnapshot in dataSnapshot.children) {
                        Email = userSnapshot.child("username").getValue(String::class.java) ?: ""
                    }
                }
                callback(Email)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(
                    this@LoginActivity,
                    "Error: ${databaseError.message}",
                    Toast.LENGTH_SHORT
                ).show()
                callback("") // Handle error by passing an empty string
            }
        })
    }

    private fun signInWithUsername(username: String, password: String, progressDialog: AlertDialog) {
        getUserEmail(username) { userEmail ->
            if (userEmail.isNotEmpty()) {
                firebaseAuth.signInWithEmailAndPassword(userEmail, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            progressDialog.dismiss()
                            handleSuccessfulLogin(username)
                            Toast.makeText(this, "Login berhasil", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, MainActivity::class.java)
                            intent.putExtra("username", username)
                            startActivity(intent)

                            finish()
                        } else {
                            progressDialog.dismiss()
                            Toast.makeText(
                                this,
                                "Username dan Password tidak sesuai",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            } else {
                progressDialog.dismiss()
                Toast.makeText(
                    this,
                    "Pengguna dengan username $username belum terdaftar",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun getUserEmail(username: String, callback: (String) -> Unit) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("Pengguna")

        val query = databaseReference.orderByChild("username").equalTo(username)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var userEmail = ""

                if (dataSnapshot.exists()) {
                    for (userSnapshot in dataSnapshot.children) {
                        userEmail = userSnapshot.child("email").getValue(String::class.java) ?: ""
                    }
                }

                callback(userEmail)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(
                    this@LoginActivity,
                    "Error: ${databaseError.message}",
                    Toast.LENGTH_SHORT
                ).show()
                callback("")
            }
        })
    }

    private fun moveToRegisterActivity() {
        startActivity(Intent(this, RegisterActivity::class.java))
        finish()
    }
}