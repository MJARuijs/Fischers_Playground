package com.mjaruijs.fischersplayground.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.util.Logger

class GoogleLoginActivity : AppCompatActivity() {

    private val activityName = "google_login_activity"

    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestId()
            .requestEmail()
            .requestIdToken(getString(R.string.server_client_id))
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, signInOptions)
        val account = GoogleSignIn.getLastSignedInAccount(this)

        if (account != null) {
            Logger.info(activityName, "Id: ${account.id}. IdToken: ${account.idToken}. Email: ${account.email}.")

            startActivity(Intent(this, MainActivity::class.java))
            return
        }

        setContentView(R.layout.activity_google_login)
        findViewById<SignInButton>(R.id.google_sign_in_button).setOnClickListener {
            signIn()
        }

    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            Logger.info(activityName, "Id: ${account.id}. IdToken: ${account.idToken}. Email: ${account.email}.")
        } catch (e: ApiException) {
            Logger.warn(activityName, "SignInResult: FAILED. ${e.statusCode}")
        }
    }

    companion object {
        private const val RC_SIGN_IN = 1
    }

}