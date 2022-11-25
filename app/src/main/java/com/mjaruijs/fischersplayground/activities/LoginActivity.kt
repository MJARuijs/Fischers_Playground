package com.mjaruijs.fischersplayground.activities

import android.content.Intent
import android.graphics.Color
import android.os.AsyncTask
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.cardview.widget.CardView
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.dialogs.CreateAccountDialog
import com.mjaruijs.fischersplayground.dialogs.SingleButtonDialog
import com.mjaruijs.fischersplayground.networking.message.NetworkMessage
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.opengl.OBJLoader
import com.mjaruijs.fischersplayground.services.LoadResourcesWorker
import com.mjaruijs.fischersplayground.userinterface.UIButton2
import com.mjaruijs.fischersplayground.util.Logger

class LoginActivity : ClientActivity() {

    override var activityName = "login_activity"

    private val createAccountDialog = CreateAccountDialog()

    private lateinit var unknownEmailDialog: SingleButtonDialog
    private lateinit var accountAlreadyExistsDialog: SingleButtonDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES)

        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        loadResources()

//        Thread {
//            preloadModels()
//            preloadTextures()
//        }.start()


//        startActivity(Intent(this, MainActivity::class.java))


        val userId = getPreference(USER_PREFERENCE_FILE).getString(USER_ID_KEY, "")!!
        if (userId.isNotBlank()) {
            stayingInApp = true
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            setContentView(R.layout.login_activity)

            createAccountDialog.create(this)
            createAccountDialog.setLayout()

            setupUIElements()
        }
    }

    override fun onResume() {
        super.onResume()
        unknownEmailDialog = SingleButtonDialog(this, "Unknown Email", "No account exists with this email", R.drawable.check_mark_icon)
        accountAlreadyExistsDialog = SingleButtonDialog(this, "Account Already Exists", "There already exists an account for this email address!", R.drawable.check_mark_icon)
    }

    override fun onMessageReceived(topic: Topic, content: Array<String>, messageId: Long) {
        when (topic) {
            Topic.SET_ID -> onIdReceived(content)
            Topic.SET_ID_AND_NAME -> onIdAndNameReceived(content)
            Topic.UNKNOWN_EMAIL -> onUnknownEmail()
            Topic.ACCOUNT_ALREADY_EXISTS -> onAccountAlreadyExists()
            else -> super.onMessageReceived(topic, content, messageId)
        }
    }

    private fun onIdReceived(content: Array<String>) {
        val id = content[0]

        this.userId = id

        savePreference(USER_ID_KEY, id)

        val token = getPreference(FIRE_BASE_PREFERENCE_FILE).getString("token", "")!!
        networkManager.sendMessage(NetworkMessage(Topic.FIRE_BASE_TOKEN, "$id|$token"))

        stayingInApp = true
        startActivity(Intent(this, MainActivity::class.java))
    }

    private fun onIdAndNameReceived(content: Array<String>) {
        val id = content[0]
        val userName = content[1]

        this.userId = id
        this.userName = userName

        savePreference(USER_ID_KEY, id)
        savePreference(USER_NAME_KEY, userName)

        val token = getPreference(FIRE_BASE_PREFERENCE_FILE).getString("token", "")!!
        networkManager.sendMessage(NetworkMessage(Topic.FIRE_BASE_TOKEN, "$id|$token"))

        stayingInApp = true
        startActivity(Intent(this, MainActivity::class.java))
    }

    private fun onUnknownEmail() {
        unknownEmailDialog.show()
    }

    private fun onAccountAlreadyExists() {
        accountAlreadyExistsDialog.show()
    }



    private fun setupUIElements() {
        val createAccountButton = findViewById<UIButton2>(R.id.create_account_button)
        createAccountButton.setText("Create Account")
            .setColor(Color.rgb(57, 57, 57))
            .setCornerRadius(45f)
            .setTextSize(28f)
            .setTextPadding(16)
            .setOnClickListener {
                stayingInApp = true
                createAccountDialog.show { email, userName ->
                    savePreference(USER_EMAIL_KEY, email)
                    savePreference(USER_NAME_KEY, userName)

                    networkManager.sendMessage(NetworkMessage(Topic.CREATE_ACCOUNT, "$email|$userName"))
                }
            }

        val emailInputBox = findViewById<EditText>(R.id.email_input_box)
        emailInputBox.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                stayingInApp = true
            }
        }

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

    private fun loadResources() {
        val textures = arrayOf(
            R.drawable.wood_diffuse_texture,
            R.drawable.white_pawn,
            R.drawable.white_knight,
            R.drawable.white_bishop,
            R.drawable.white_rook,
            R.drawable.white_queen,
            R.drawable.white_king,
            R.drawable.black_pawn,
            R.drawable.black_knight,
            R.drawable.black_bishop,
            R.drawable.black_rook,
            R.drawable.black_queen,
            R.drawable.black_king,
            R.drawable.diffuse_map_pawn,
            R.drawable.diffuse_map_knight,
            R.drawable.diffuse_map_bishop,
            R.drawable.diffuse_map_rook,
            R.drawable.diffuse_map_queen,
            R.drawable.diffuse_map_king,
            R.drawable.king_checked,
        )

        val models = arrayOf(
            R.raw.pawn_bytes,
            R.raw.knight_bytes,
            R.raw.bishop_bytes,
            R.raw.rook_bytes,
            R.raw.queen_bytes,
            R.raw.king_bytes
        )
//
//        val textureLoader = TextureLoader.getInstance()
//
//        for (textureId in textures) {
//            textureLoader.load(applicationContext.resources, textureId)
//        }
//
//        for (modelId in models) {
//            OBJLoader.preload(applicationContext.resources, modelId)
//        }
        val worker = OneTimeWorkRequestBuilder<LoadResourcesWorker>()
            .setInputData(
                workDataOf(
                    Pair("texture_resources", textures),
                    Pair("model_resources", models)
                )
            ).build()

        val workManager = WorkManager.getInstance(applicationContext)
        workManager.enqueue(worker)
    }

}