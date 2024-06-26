package com.bartas.aplikasibarter

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import com.bartas.aplikasibarter.databinding.FragmentAkunBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AkunFragment : Fragment() {

    private lateinit var popupbtn: AppCompatButton
    var databaseReference: DatabaseReference? = null
    private lateinit var dialog: Dialog
    private lateinit var fragmentAkunBinding: FragmentAkunBinding
    private lateinit var auth : FirebaseAuth
    private lateinit var uid : String
    private lateinit var username : String
    private lateinit var foto : String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fragmentAkunBinding = FragmentAkunBinding.inflate(inflater, container, false)
        val view = fragmentAkunBinding.root

        popupbtn = view.findViewById(R.id.btnLogout)

        dialog = Dialog(requireContext())

        auth = FirebaseAuth.getInstance()
        uid = auth.currentUser?.uid.toString()
        databaseReference = FirebaseDatabase.getInstance().getReference("Pengguna")
        if (uid.isNotEmpty()){
            getUsername()
        }

        popupbtn.setOnClickListener {
            dialog.setContentView(R.layout.popup_logout)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.show()
            dialog.setCancelable(false)

            dialog.findViewById<AppCompatButton>(R.id.btn_yes).setOnClickListener {
                clearSharedPreferences()
                auth.signOut()
                val intent = Intent(requireContext(), LoginActivity::class.java)
                startActivity(intent)
                dialog.dismiss()
            }

            dialog.findViewById<AppCompatButton>(R.id.btn_no).setOnClickListener {
                dialog.dismiss()
            }
        }

        // Retrieve user information and update UI
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val username = requireActivity().intent.getStringExtra("username")
            val email = user.email

            // Update nameTextView and emailTextView
            fragmentAkunBinding.nameTextView.text = username
            fragmentAkunBinding.emailTextView.text = email
        }

        val EditProfil = view.findViewById<AppCompatButton>(R.id.btnEditProfile)
        EditProfil.setOnClickListener {
            val intent = Intent(requireContext(), EditProfilActivity::class.java)
            startActivity(intent)
        }

        val PostinganSaya = view.findViewById<AppCompatButton>(R.id.btnPostinganSaya)
        PostinganSaya.setOnClickListener {
            val intent = Intent(requireContext(), PostinganSayaActivity::class.java)
            startActivity(intent)
        }

        val RiwayatBarter = view.findViewById<AppCompatButton>(R.id.btnRiwayatBarter)
        RiwayatBarter.setOnClickListener {
            val intent = Intent(requireContext(), BarangTerjualActivity::class.java)
            startActivity(intent)
        }

        return view
    }

    private fun getUsername() {
        databaseReference?.child(uid)?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                username = snapshot.child("username").value.toString()
                fragmentAkunBinding.nameTextView.text = username
                val photoUrl = snapshot.child("photo_url").value?.toString() ?: "@drawable/ic_person"

                if (photoUrl.startsWith("http")) {
                    // Load the image using Glide
                    Glide.with(this@AkunFragment)
                        .load(photoUrl)
                        .apply(RequestOptions().placeholder(R.drawable.foto_profil)) // Placeholder image
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(fragmentAkunBinding.imageView)
                } else {
                    // Use a default placeholder if the URL is not valid
                    fragmentAkunBinding.imageView.setImageResource(R.drawable.foto_profil)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("PostingBarangActivity", "Error saat mengambil data username dari Firebase: ${error.message}")
            }
        })
    }

    private fun clearSharedPreferences() {
        val editor = requireContext().getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE).edit()
        editor.putBoolean(LoginActivity.KEY_IS_LOGGED_IN, false)
        editor.remove("username") // Remove the username
        editor.apply()
    }
}