package com.willowtreeapps.signinwithapplebutton.view

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.widget.Button
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.core.content.ContextCompat
import com.willowtreeapps.signinwithapplebutton.AppleSignInCallback
import com.willowtreeapps.signinwithapplebutton.R
import java.util.*
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import com.willowtreeapps.signinwithapplebutton.*

@SuppressLint("SetJavaScriptEnabled")
class SignInWithAppleButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0, defStyleRes: Int = 0
) : Button(context, attrs, defStyleAttr, defStyleRes) {
    var redirectUri: String = ""
    var clientId: String = ""
    var state: String = UUID.randomUUID().toString()
    //TODO: Figure out the behavior/default for scope; default was "email name"
    var scope: String = ""

    var callback: AppleSignInCallback? = null

    var tabPackage: String? = null

    private val connection = object : CustomTabsServiceConnection() {
        override fun onCustomTabsServiceConnected(name: ComponentName?, client: CustomTabsClient?) {
            client?.warmup(0L)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    internal companion object {
        const val SIGN_IN_WITH_APPLE_LOG_TAG = "SIGN_IN_WITH_APPLE"
    }

    fun bindService(context: Context) {
        CustomTabsClient.bindCustomTabsService(context, CustomTabsClient.getPackageName(context, null), connection)
    }

    fun unbindService(context: Context) {
        context.unbindService(connection)
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.sign_in_with_apple_button, this, true)
    }

    private val imageView: ImageView = findViewById(R.id.imageView)
    private val textView: TextView = findViewById(R.id.textView)

    init {
        val attributes =
            context.theme.obtainStyledAttributes(attrs, R.styleable.SignInWithAppleButton, 0, R.style.SignInWithAppleButton)

        // Style
        val background = attributes.getDrawable(R.styleable.SignInWithAppleButton_android_background)
        val icon = attributes.getResourceId(R.styleable.SignInWithAppleButton_android_drawableLeft, -1)
        val iconSize = attributes.getDimensionPixelSize(R.styleable.SignInWithAppleButton_sign_in_with_apple_button_iconSize, -1)
        val textColor = attributes.getColorStateList(R.styleable.SignInWithAppleButton_android_textColor)
        val textSize = attributes.getDimensionPixelSize(R.styleable.SignInWithAppleButton_android_textSize, -1)
        val textStyle = attributes.getInt(R.styleable.SignInWithAppleButton_android_textStyle, 0)
        val fontFamily = attributes.getString(R.styleable.SignInWithAppleButton_android_fontFamily)
        val textAppearance = attributes.getResourceId(R.styleable.SignInWithAppleButton_android_textAppearance, -1)

        // Text type
        val text = attributes.getInt(
            R.styleable.SignInWithAppleButton_sign_in_with_apple_button_textType,
            SignInTextType.SIGN_IN.ordinal
        )

        // Hide or show icon
        val enableIcon = attributes.getBoolean(
            R.styleable.SignInWithAppleButton_sign_in_with_apple_button_enableIcon,
            true
        )

        // Corner radius
        val cornerRadius = attributes.getDimension(
            R.styleable.SignInWithAppleButton_sign_in_with_apple_button_cornerRadius,
            resources.getDimension(R.dimen.sign_in_with_apple_button_cornerRadius_default)
        )

        attributes.recycle()

        this.background = background?.mutate()
        (background as? GradientDrawable)?.cornerRadius = cornerRadius

        if (icon != -1 && enableIcon) {
            imageView.setImageResource(icon)

            if (iconSize != -1) {
                imageView.layoutParams.width = iconSize
                imageView.layoutParams.height = iconSize
            }
        } else {
            imageView.visibility = View.GONE
        }

        setPaddingRelative(
            padding - compoundDrawablePadding, padding - compoundDrawablePadding,
            padding, padding - compoundDrawablePadding
        )
        textView.setTextColor(textColor)

        if (textSize != -1) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize.toFloat())
        }

        setOnClickListener {
            buildCustomTabIntent().launchUrl(context, buildUri())
        }

        // set default typeface
        val typeface = if (fontFamily == null) {
            Typeface.create(textView.typeface, textStyle)
        } else {
            Typeface.create(fontFamily, textStyle)
        }

        // textView.typeface = typeface
        if (textAppearance != -1) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                textView.setTextAppearance(textAppearance)
            } else {
                // not compatible with lollipop below
                setTypeFace(typeface)
            }
        } else {
            // default
            setTypeFace(typeface)
        }

        textView.text = resources.getString(SignInTextType.values()[text].text)
    }

    /*
    We have to build this URI out ourselves because the default behavior is to POST the response, while we
    need a GET so that we can retrieve the code (and potentially ID token/state). The URI created is based off
    the URI constructed by Apple's Javascript SDK, and is why certain fields (like the version, v) are included
    in the URI construction.

    See the Sign In With Apple Javascript SDK for reference:
    https://developer.apple.com/documentation/signinwithapplejs/configuring_your_webpage_for_sign_in_with_apple
    */
    private fun buildUri() = Uri
        .parse("https://appleid.apple.com/auth/authorize")
        .buildUpon().apply {
            appendQueryParameter("response_type", "code")
            appendQueryParameter("v", "1.1.6")
            appendQueryParameter("redirect_uri", redirectUri)
            appendQueryParameter("client_id", clientId)
            appendQueryParameter("scope", scope)
            appendQueryParameter("state", state)
        }.build()

    private fun buildCustomTabIntent(): CustomTabsIntent {
        val customTabsIntent = CustomTabsIntent.Builder().apply {
            setToolbarColor(ContextCompat.getColor(context, R.color.black))
            setSecondaryToolbarColor(ContextCompat.getColor(context, R.color.white))
        }.build()
        customTabsIntent.intent.`package` = tabPackage
        return customTabsIntent
    }

    fun setTypeFace(typeface: Typeface) {
        textView.typeface = typeface
    }

    fun setUpSignInWithAppleOnClick(
        fragmentManager: FragmentManager,
        configuration: SignInWithAppleConfiguration,
        callback: (SignInWithAppleResult) -> Unit
    ) {
        val fragmentTag = "SignInWithAppleButton-$id-SignInWebViewDialogFragment"
        val service = SignInWithAppleService(fragmentManager, fragmentTag, configuration, callback)
        setOnClickListener { service.show() }
    }

    fun setUpSignInWithAppleOnClick(
        fragmentManager: FragmentManager,
        configuration: SignInWithAppleConfiguration,
        callback: SignInWithAppleCallback
    ) {
        setUpSignInWithAppleOnClick(fragmentManager, configuration, callback.toFunction())
    }
}
