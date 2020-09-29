package com.willowtreeapps.signinwithapplebutton

data class SignInWithAppleConfiguration(
    val clientId: String,
    val redirectUri: String,
    val scope: String,
    val verifyState: Boolean
) {

    class Builder {
        // Client ID
        private lateinit var clientId: String
        // Redirect URL string
        private lateinit var redirectUri: String
        // Scope (mostly "name email")
        private lateinit var scope: String
        // Workaround: Current Apple REST API doesn't
        // return the state although we passed the state hash generated from our end.
        private var verifyState: Boolean = true

        fun clientId(clientId: String) = apply {
            this.clientId = clientId
        }

        fun redirectUri(redirectUri: String) = apply {
            this.redirectUri = redirectUri
        }

        fun scope(scope: String) = apply {
            this.scope = scope
        }

        fun verifyState(verifyState: Boolean) = apply {
            this.verifyState = verifyState
        }

        fun build() = SignInWithAppleConfiguration(clientId, redirectUri, scope, verifyState)
    }
}
