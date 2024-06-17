package com.example.messagingapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.messagingapp.details.Message
import com.example.messagingapp.details.MessageAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.security.Key
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class ChatActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var messageInput: EditText
    private lateinit var sendButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var messageList: MutableList<Message>
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var chatId: String
    private lateinit var secretKey: Key
    private lateinit var chatHeader: TextView
    private var userId: String? = null
    private var userName: String? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("chats")
        userId = intent.getStringExtra("userId")
        userName = intent.getStringExtra("userName")
        chatId = generateChatId(auth.currentUser!!.uid, userId!!)

        secretKey = generateFixedKey()

        chatHeader = findViewById(R.id.chatHeader)
        chatHeader.text = "Chatting with $userName"

        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)
        recyclerView = findViewById(R.id.recyclerView)
        messageList = mutableListOf()
        messageAdapter = MessageAdapter(messageList, auth.currentUser!!.uid)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = messageAdapter

        sendButton.setOnClickListener {
            sendMessage()
        }

        listenForMessages()
    }

    private fun sendMessage() {
        val messageText = messageInput.text.toString()
        if (!TextUtils.isEmpty(messageText)) {
            try {
                val encryptedMessage = encrypt(messageText, secretKey)
                Log.d("ChatActivity", "Encrypted Message: $encryptedMessage")
                val message = Message(auth.currentUser!!.uid, encryptedMessage)
                database.child(chatId).push().setValue(message)
                messageInput.setText("")
                incrementNewMessageCount()
                scrollToBottom()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun listenForMessages() {
        database.child(chatId).addChildEventListener(object : ChildEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(Message::class.java)
                if (message != null) {
                    try {
                        val decryptedMessage = decrypt(message.text!!, secretKey)
                        message.text = decryptedMessage
                        messageList.add(message)
                        messageAdapter.notifyDataSetChanged()
                        markMessagesAsRead()
                        scrollToBottom()
                    } catch (e: Exception) {
                        Log.e("ChatActivity", "Decryption failed", e)
                    }
                }
                markMessagesAsRead()
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatActivity", "Error: ${error.message}")
            }
        })
    }


    private fun markMessagesAsRead() {
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(auth.currentUser!!.uid)
        userRef.child("newMessageCount").setValue(0)
    }

    private fun incrementNewMessageCount() {
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(auth.currentUser!!.uid)
        userRef.child("newMessageCount").get().addOnSuccessListener {
            val currentCount = it.getValue(Int::class.java) ?: 0
            userRef.child("newMessageCount").setValue(currentCount + 1)
        }
    }

    private fun scrollToBottom() {
        recyclerView.post {
            recyclerView.smoothScrollToPosition(messageAdapter.itemCount - 1)
        }
    }

    private fun generateChatId(user1: String, user2: String): String {
        val userIds = listOf(user1, user2).sorted()
        return "${userIds[0]}-${userIds[1]}"
    }

    private fun generateFixedKey(): Key {
        val keyString = "12345678901234567890123456789012"
        return SecretKeySpec(keyString.toByteArray(), "AES")
    }

    private fun encrypt(plainText: String, secretKey: Key): String {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val encryptedBytes = cipher.doFinal(plainText.toByteArray())
        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
    }

    private fun decrypt(encryptedText: String, secretKey: Key): String {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        val encryptedBytes = Base64.decode(encryptedText, Base64.DEFAULT)
        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return String(decryptedBytes)
    }
}
