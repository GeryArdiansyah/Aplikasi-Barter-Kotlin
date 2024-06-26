package com.bartas.aplikasibarter

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import com.bartas.aplikasibarter.Adapter.ChatBukaAdapter
import com.bartas.aplikasibarter.Models.DataChatMessage
import com.bartas.aplikasibarter.databinding.ActivityBukaChatBinding
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

class BukaChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBukaChatBinding
    private lateinit var popupWindow: PopupWindow
    private lateinit var chatAdapter: ChatBukaAdapter
    private lateinit var currentUserUid: String
    private lateinit var receiverUid: String
    private lateinit var databaseReference: DatabaseReference
    private lateinit var storageReference: StorageReference
    private lateinit var toolbar: Toolbar
    private lateinit var txtjam: TextView
    private val CAMERA_REQUEST_CODE = 100
    private val IMAGE_REQUEST_CODE = 200
    private val MAPS_ACTIVITY_REQUEST_CODE = 300
    private var isTombolTambahActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBukaChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        toolbar = findViewById<Toolbar>(R.id.toolbarBack)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        val auth = FirebaseAuth.getInstance()
        currentUserUid = auth.currentUser?.uid ?: ""


        // Get user details from intent
        val username = intent.getStringExtra("username")
        val photoUrl = intent.getStringExtra("photo_url")
        receiverUid = intent.getStringExtra("receiverUid") ?: ""

        fetchUserDetails(receiverUid)

        val textViewUsername: TextView = binding.userName
        val imageViewProfile: ImageView = binding.profilbuka
        textViewUsername.text = username
        txtjam = binding.deskripsi

        Glide.with(this)
            .load(photoUrl)
            .placeholder(R.drawable.foto_profil)
            .into(imageViewProfile)

        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        binding.chatRecyclerView.layoutManager = layoutManager

        // Initialize adapter and RecyclerView
        chatAdapter = ChatBukaAdapter(currentUserUid)
        binding.chatRecyclerView.adapter = chatAdapter

        // Initialize database reference to the "messages" node for the current user
        databaseReference = FirebaseDatabase.getInstance().reference.child("messages")
        // Initialize storage reference to Firebase Storage
        storageReference = FirebaseStorage.getInstance().reference.child("your_storage_path")

        getMessagesFromFirebase()

        binding.tombolkirim.setOnClickListener {
            val messageText = binding.textpesan.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
            }
        }

        val popupView = layoutInflater.inflate(R.layout.popup_chat, null)
        popupWindow = PopupWindow(popupView, 370.dpToPx(), 178.dpToPx(), true)
        popupWindow.setBackgroundDrawable(null)

        popupWindow.setOnDismissListener {
            isTombolTambahActive = false
            updateTombolTambahImage()
        }

        binding.tomboltambah.setOnClickListener {
            isTombolTambahActive = !isTombolTambahActive
            updateTombolTambahImage()
            if (isTombolTambahActive) {
                val offset = -(178.dpToPx() + binding.tomboltambah.height + 20.dpToPx())
                popupWindow.showAsDropDown(binding.tomboltambah, 0, offset)
            } else {
                popupWindow.dismiss()
            }
        }

        val btnKamera = popupView.findViewById<AppCompatButton>(R.id.btn_kamera)
        val btnGambar = popupView.findViewById<AppCompatButton>(R.id.btn_gambar)
        val btnLokasi = popupView.findViewById<AppCompatButton>(R.id.btn_lokasi)

        btnKamera.setOnClickListener {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
        }

        btnGambar.setOnClickListener {
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(galleryIntent, IMAGE_REQUEST_CODE)
        }

        btnLokasi.setOnClickListener {
            // Start MapsActivity untuk pemilihan lokasi
            val intent = Intent(this, MapsActivity::class.java)
            startActivityForResult(intent, MAPS_ACTIVITY_REQUEST_CODE)
        }
    }

    private fun fetchUserDetails(userId: String) {
        val userReference = FirebaseDatabase.getInstance().reference.child("Pengguna").child(userId)
        userReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val username = snapshot.child("username").value.toString()
                    val photoUrl = snapshot.child("photo_url").value.toString()

                    // Set the username and photo in UI
                    val textViewUsername: TextView = binding.userName
                    val imageViewProfile: ImageView = binding.profilbuka

                    textViewUsername.text = username

                    Glide.with(this@BukaChatActivity)
                        .load(photoUrl)
                        .placeholder(R.drawable.foto_profil)
                        .into(imageViewProfile)
                } else {
                    // Handle the case where user details are not found
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle errors if needed
            }
        })
    }

    private fun updateTombolTambahImage() {
        val drawableResId = if (isTombolTambahActive) {
            R.drawable.vektor_kali
        } else {
            R.drawable.vektor_tambah
        }
        binding.tomboltambah.setImageResource(drawableResId)
    }

    private fun Int.dpToPx(): Int {
        val density = resources.displayMetrics.density
        return (this * density).toInt()
    }

    private fun getMessagesFromFirebase() {
        databaseReference.orderByChild("timestamp").addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val chatMessage = snapshot.getValue(DataChatMessage::class.java)
                if (chatMessage != null) {
                    val isMessageForCurrentUser =
                        chatMessage.senderUid == currentUserUid && chatMessage.receiverUid == receiverUid ||
                                chatMessage.senderUid == receiverUid && chatMessage.receiverUid == currentUserUid

                    if (isMessageForCurrentUser) {
                        chatAdapter.addMessage(chatMessage)
                        binding.textpesan.text.clear()
                        updateRecipientLastActiveTime()
                    }
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                // Handle changes if needed
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                // Handle removal if needed
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // Handle movement if needed
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle errors if needed
            }
        })
    }

    private fun getCurrentTime(): String {
        val currentTimeMillis = System.currentTimeMillis()
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(currentTimeMillis))
    }

    private fun sendMessage(messageText: String) {
        val currentTime = getCurrentTime()
        val messageId = databaseReference.push().key ?: ""
        val senderUid = currentUserUid
        val receiverUid = receiverUid

        val senderMessage = DataChatMessage(senderUid, receiverUid, "You", messageText, currentTime)

        databaseReference.child(messageId).setValue(senderMessage)
    }

    private fun sendMessageWithImage(messageText: String, imageUrl: String) {
        val currentTime = getCurrentTime()
        val messageId = databaseReference.push().key ?: ""
        val senderUid = currentUserUid
        val receiverUid = receiverUid

        // Gunakan pesan kosong atau sesuai kebutuhan
        val senderMessage = DataChatMessage(senderUid, receiverUid, "You", "", currentTime, imageUrl)

        databaseReference.child(messageId).setValue(senderMessage)
    }

    private fun uploadImageToFirebaseStorage(bitmap: Bitmap) {
        val imageRef = storageReference.child("${UUID.randomUUID()}.jpg")

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val imageData = baos.toByteArray()

        val uploadTask = imageRef.putBytes(imageData)
        uploadTask.addOnSuccessListener {
            // Image upload success
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                // Get the download URL and send the message with the image URL
                val imageUrl = uri.toString()
                sendMessageWithImage("Sent an image", imageUrl)
            }
        }.addOnFailureListener {
            // Handle the failure
        }
    }

    private fun uploadImageToFirebaseStorageFromUri(imageUri: Uri?) {
        if (imageUri != null) {
            val imageRef = storageReference.child("${UUID.randomUUID()}.jpg")

            val uploadTask = imageRef.putFile(imageUri)
            uploadTask.addOnSuccessListener {
                // Image upload success
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    // Get the download URL and send the message with the image URL
                    val imageUrl = uri.toString()
                    sendMessageWithImage("Sent an image", imageUrl)
                }
            }.addOnFailureListener {
                // Handle the failure
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            MAPS_ACTIVITY_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    // Dapatkan informasi lokasi dari MapsActivity
                    val selectedLocation = data?.getStringExtra("selectedLocation") ?: ""
                    // Kirim lokasi ke pengguna lain melalui pesan
                    sendMessage(selectedLocation)
                }
            }

            CAMERA_REQUEST_CODE -> {
                if (resultCode == RESULT_OK) {
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    // Upload gambar ke Firebase Storage dan kirim URL gambar
                    uploadImageToFirebaseStorage(imageBitmap)
                }
            }

            IMAGE_REQUEST_CODE -> {
                if (resultCode == RESULT_OK) {
                    val imageUri = data?.data
                    // Upload gambar ke Firebase Storage dan kirim URL gambar
                    uploadImageToFirebaseStorageFromUri(imageUri)
                }
            }
        }
    }

    private fun updateRecipientLastActiveTime() {
        val currentTime = System.currentTimeMillis()
        val userReference = FirebaseDatabase.getInstance().reference.child("users").child(receiverUid)
        userReference.child("lastActiveTime").setValue(currentTime)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Update txtjam with the current time
                    txtjam.text = getCurrentTime()
                } else {
                    // Handle the error
                    Toast.makeText(
                        this@BukaChatActivity,
                        "Failed to update last active time",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }


    override fun onDestroy() {
        super.onDestroy()
        popupWindow.dismiss()
    }
}