package com.willowtreeapps.signinwithapplebutton.view

import android.accounts.NetworkErrorException
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.willowtreeapps.signinwithapplebutton.SignInWithAppleConfiguration
import com.willowtreeapps.signinwithapplebutton.SignInWithAppleResult
import com.willowtreeapps.signinwithapplebutton.SignInWithAppleService
import com.willowtreeapps.signinwithapplebutton.constants.Strings
import com.willowtreeapps.signinwithapplebutton.view.SignInWithAppleButton.Companion.SIGN_IN_WITH_APPLE_LOG_TAG

internal class SignInWebViewClient(
    private val fragment: SignInWebViewDialogFragment,
    private val configuration: SignInWithAppleConfiguration,
    private val attempt: SignInWithAppleService.AuthenticationAttempt,
    private val callback: (SignInWithAppleResult) -> Unit
) : WebViewClient() {

    private val TAG: String = ::SignInWebViewClient.javaClass.simpleName

    // for API levels < 24
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        return isUrlOverridden(view, Uri.parse(url))
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        return isUrlOverridden(view, request?.url)
    }

    // TODO: Optimizations
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onPageStarted(view: WebView?, url: String, favicon: Bitmap?) {
        fragment.updateSubtitle(url)
        super.onPageStarted(view, url, favicon)
    }

    // TODO: Optimizations
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onPageFinished(view: WebView?, url: String) {
        hideProgress()
        super.onPageFinished(view, url)
    }

    override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, url: String) {
        Log.d(TAG, "onReceivedError: $url")
        callback(SignInWithAppleResult.Failure(NetworkErrorException("onReceivedError")))
    }

    private fun isUrlOverridden(view: WebView?, url: Uri?): Boolean {
        /*
         * This fixes the issue:
         * https://github.com/willowtreeapps/sign-in-with-apple-button-android/issues/68
         */
        val redirectUrlResult: String = attempt.redirectUri.replace("/handler", "/result")

        return when {
            url == null -> {
                false
            }
            url.toString().contains(Strings.APPLEID_URL) -> {
                view?.loadUrl(url.toString())
                true
            }
            url.toString().contains(redirectUrlResult) -> {
                setCallback(url)
                true
            }
            else -> {
                false
            }
        }
    }

    private fun setCallback(url: Uri) {
        Log.d(SIGN_IN_WITH_APPLE_LOG_TAG, "Web view was forwarded to redirect URI")
        val codeParameter = url.getQueryParameter("uuid") // based on thorough test of latest Apple's REST API
        val stateParameter = url.getQueryParameter("state") // WARN: not exists on the latest Apple's REST API
        val authParameter = url.getQueryParameter("auth")
        // parse scopes from url
        val scopes: Array<String> = configuration.scope.split(" ").toTypedArray()
        val scopesMutable = mutableMapOf<String, String>()
        for (scope in scopes) {
            scopesMutable[scope] = url.getQueryParameter(scope).toString()
        }
        when {
            codeParameter == null -> {
                callback(SignInWithAppleResult.Failure(IllegalArgumentException("code not returned")))
            }
            // Workaround: make sure that the verifyState is true and stateParameter does not exists
            configuration.verifyState && stateParameter != null && stateParameter != attempt.state -> {
                callback(SignInWithAppleResult.Failure(IllegalArgumentException("state does not match")))
            }
            authParameter == "success" -> {
                callback(SignInWithAppleResult.Success(codeParameter, scopesMutable))
            }
            else -> {
                // TODO: Needs citing for this error message
                callback(SignInWithAppleResult.Failure(IllegalArgumentException("access denied")))
            }
        }
    }

    private fun hideProgress() {
        fragment.hideProgress()
    }

}
