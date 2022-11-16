package com.mjaruijs.fischersplayground.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.cardview.widget.CardView
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.dialogs.CreateAccountDialog
import com.mjaruijs.fischersplayground.networking.message.NetworkMessage
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.userinterface.UIButton2
import com.mjaruijs.fischersplayground.util.Logger

class LoginActivity : ClientActivity() {

    private val createAccountDialog = CreateAccountDialog()

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES)

        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        val userId = getPreference(USER_PREFERENCE_FILE).getString(USER_ID_KEY, "")!!
        if (userId.isNotBlank()) {
//            stayingInApp = true
//            startActivity(Intent(this, MainActivity::class.java))
//            return
        }

        setContentView(R.layout.login_activity)

        createAccountDialog.create(this)
        createAccountDialog.setLayout()

        setupUIElements()
    }

    override fun onMessageReceived(topic: Topic, content: Array<String>, messageId: Long) {
        when (topic) {
            Topic.SET_ID -> onIdReceived(content)
            Topic.SET_ID_AND_NAME -> onIdAndNameReceived(content)
            else -> super.onMessageReceived(topic, content, messageId)
        }
    }

    private fun onIdReceived(content: Array<String>) {
        val id = content[0]

        savePreference(USER_ID_KEY, id)

        val token = getPreference(FIRE_BASE_PREFERENCE_FILE).getString("token", "")!!
        networkManager.sendMessage(NetworkMessage(Topic.FIRE_BASE_TOKEN, "$id|$token"))

        stayingInApp = true
        startActivity(Intent(this, MainActivity::class.java))
    }

    private fun onIdAndNameReceived(content: Array<String>) {
        val id = content[0]
        val userName = content[1]

        savePreference(USER_ID_KEY, id)
        savePreference(USER_NAME_KEY, userName)

//        if (hasNewToken) {
        val token = getPreference(FIRE_BASE_PREFERENCE_FILE).getString("token", "")!!
        networkManager.sendMessage(NetworkMessage(Topic.FIRE_BASE_TOKEN, "$id|$token"))

        stayingInApp = true
        startActivity(Intent(this, MainActivity::class.java))
    //        }
    }

    private fun setupUIElements() {
        val createAccountButton = findViewById<UIButton2>(R.id.create_account_button)
        createAccountButton.setText("Create Account")
            .setColor(Color.rgb(235, 186, 145))
            .setCornerRadius(45f)
            .setTextSize(28f)
            .setOnClickListener {
                stayingInApp = true
                createAccountDialog.show { email, userName ->
                    savePreference(USER_EMAIL_KEY, email)
                    savePreference(USER_NAME_KEY, userName)

                    networkManager.sendMessage(NetworkMessage(Topic.CREATE_ACCOUNT, "$email|$userName"))
                }
            }

        val emailInputBox = findViewById<EditText>(R.id.email_input_box)
        emailInputBox.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                stayingInApp = true
            }
        }
//        emailInputBox.setOnClickListener {
//            Logger.debug("MyTag", "Click!")
//            stayingInApp = true
//        }

        val loginButton = findViewById<CardView>(R.id.login_button)
        loginButton.setOnClickListener {
            networkManager.sendMessage(NetworkMessage(Topic.EMAIL_LOGIN, emailInputBox.text.toString()))
        }
    }

    private fun savePreference(key: String, value: String) {
        val preferences = getPreference(USER_PREFERENCE_FILE)

        with(preferences.edit()) {
            putString(key, value)
            apply()
        }
    }

}