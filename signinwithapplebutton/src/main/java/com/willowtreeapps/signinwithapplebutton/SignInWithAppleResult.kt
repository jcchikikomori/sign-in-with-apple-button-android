package com.willowtreeapps.signinwithapplebutton

sealed class SignInWithAppleResult {

    // Success with authorization code & scopes
    data class Success(val code: String, val scopes: MutableMap<String, String>) : SignInWithAppleResult()

    // Failure with throwable exception
    data class Failure(val error: Throwable) : SignInWithAppleResult()

    object Cancel : SignInWithAppleResult()
}
