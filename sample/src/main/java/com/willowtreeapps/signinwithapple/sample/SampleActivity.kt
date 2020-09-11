package com.willowtreeapps.signinwithapple.sample

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.appcompat.app.AppCompatActivity
import com.willowtreeapps.signinwithapplebutton.SignInWithAppleConfiguration
import com.willowtreeapps.signinwithapplebutton.SignInWithAppleResult
import com.willowtreeapps.signinwithapplebutton.view.SignInWithAppleButton

class SampleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample)
        signInWithAppleButtonBlack = findViewById(R.id.sign_in_with_apple_button_black)
        signInWithAppleButtonWhite = findViewById(R.id.sign_in_with_apple_button_white)
        signInWithAppleButtonWhiteOutline = findViewById(R.id.sign_in_with_apple_button_white_outline)

        signInWithAppleButtonBlack.bindService(this)
        signInWithAppleButtonWhite.bindService(this)
        signInWithAppleButtonWhiteOutline.bindService(this)
    }

    override fun onResume() {
        super.onResume()
        setUpSignInButton(signInWithAppleButtonBlack)
        setUpSignInButton(signInWithAppleButtonWhite)
        setUpSignInButton(signInWithAppleButtonWhiteOutline)
    }

    override fun onStop() {
        super.onStop()
        signInWithAppleButtonBlack.callback = null
        signInWithAppleButtonWhite.callback = null
        signInWithAppleButtonWhiteOutline.callback = null
    }

    override fun onDestroy() {
        super.onDestroy()
        signInWithAppleButtonBlack.unbindService(this)
        signInWithAppleButtonWhite.unbindService(this)
        signInWithAppleButtonWhiteOutline.unbindService(this)
    }

    private fun setUpSignInButton(button: SignInWithAppleButton) = button.apply {
        //TODO: Replace redirectUri and clientId with your own values
        redirectUri = "https://kconner.com/sign-in-with-apple-button-android-example-app/callback"
        clientId = "com.kevinconner.sign-in-with-apple-button-android-example-site"
        scope = "email name"
//        redirectUri = "com.your.client.id.here"
//        clientId = "https://your-redirect-uri.com/callback"
//        scope = "email name"
        callback = signInCallback

        val signInWithAppleButtonBlack: SignInWithAppleButton = findViewById(R.id.sign_in_with_apple_button_black)
        val signInWithAppleButtonWhite: SignInWithAppleButton = findViewById(R.id.sign_in_with_apple_button_white)
        val signInWithAppleButtonWhiteOutline: SignInWithAppleButton = findViewById(R.id.sign_in_with_apple_button_white_outline)

        // Replace clientId and redirectUri with your own values.
        val configuration = SignInWithAppleConfiguration(
            clientId = "com.your.client.id.here",
            redirectUri = "https://your-redirect-uri.com/callback",
            scope = "email name"
        )

        val callback: (SignInWithAppleResult) -> Unit = { result ->
            when (result) {
                is SignInWithAppleResult.Success -> {
                    Toast.makeText(this, result.authorizationCode, LENGTH_SHORT).show()
                }
                is SignInWithAppleResult.Failure -> {
                    Log.d("SAMPLE_APP", "Received error from Apple Sign In ${result.error.message}")
                }
                is SignInWithAppleResult.Cancel -> {
                    Log.d("SAMPLE_APP", "User canceled Apple Sign In")
                }
            }
        }

        signInWithAppleButtonBlack.setUpSignInWithAppleOnClick(supportFragmentManager, configuration, callback)
        signInWithAppleButtonWhite.setUpSignInWithAppleOnClick(supportFragmentManager, configuration, callback)
        signInWithAppleButtonWhiteOutline.setUpSignInWithAppleOnClick(supportFragmentManager, configuration, callback)
    }
}
