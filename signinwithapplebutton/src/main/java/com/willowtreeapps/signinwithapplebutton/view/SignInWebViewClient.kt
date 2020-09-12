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

/**
 * Will do some of the work here:
 * - Use shouldOverrideUrlLoading for checking redirect URL & current URL by using .startsWith()
 */
internal class SignInWebViewClient(
    private val fragment: SignInWebViewDialogFragment,
    private val config: SignInWithAppleConfiguration,
    private val attempt: SignInWithAppleService.AuthenticationAttempt,
    private val callback: (SignInWithAppleResult) -> Unit
) : WebViewClient() {

    private val TAG: String = ::SignInWebViewClient.javaClass.simpleName
    private var success: Boolean = false
    private var failed: Boolean = false
    private var gotError: Boolean = false

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
        setCallback(Uri.parse(url))
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
        gotError = true // set status to avoid override by onPageFinished()
        callback(SignInWithAppleResult.Failure(NetworkErrorException("onReceivedError")))
    }

    private fun isUrlOverridden(view: WebView?, url: Uri?): Boolean {
        return when {
            url == null -> {
                false
            }
            url.toString().contains(Strings.APPLEID_URL) -> {
                view?.loadUrl(url.toString())
                true
            }
            else -> {
                false
            }
        }
    }

    private fun setCallback(url: Uri) {
        // workaround
        val redirectUrlResult: String = attempt.redirectUri.replace("/handler", "/result")
        if (url.toString().contains(redirectUrlResult)) {
            // TODO: getQueryParameter by scope
            Log.d(SIGN_IN_WITH_APPLE_LOG_TAG, "Web view was forwarded to redirect URI")
            val codeParameter = url.getQueryParameter("uuid") // based on thorough test of latest Apple's REST API
            // val stateParameter = url.getQueryParameter("state") // not exists on the latest Apple's REST API
            val authParameter = url.getQueryParameter("auth")
            // parse scopes
            val scopeParameter: String = config.scope
            val scopes: Array<String> = scopeParameter.split(" ").toTypedArray()
            val scopesMutable = mutableMapOf<String, String>()
            for (scope in scopes) {
                scopesMutable[scope] = url.getQueryParameter(scope).toString()
            }
            when {
                codeParameter == null -> {
                    callback(SignInWithAppleResult.Failure(IllegalArgumentException("code not returned")))
                }
                /*
                stateParameter != attempt.state || authParameter != attempt.state -> {
                    callback(SignInWithAppleResult.Failure(IllegalArgumentException("state does not match")))
                }
                 */
                // workaround
                authParameter == "success" -> {
                    attempt.state
                    callback(SignInWithAppleResult.Success(codeParameter, scopesMutable))
                }
                else -> {
                    callback(SignInWithAppleResult.Success(codeParameter, scopesMutable))
                }
            }
        } else {
            Toast.makeText(
                fragment.context,
                "onPageStarted: $url \n" + "redirectUrl: ${attempt.redirectUri}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun hideProgress() {
        // val progress: ProgressBar findViewById(R.id.a)
        // progress.setVisibility(View.GONE)
        Log.e(TAG, "NO PROGRESS BAR YET!")
        fragment.hideProgress()
    }

}
