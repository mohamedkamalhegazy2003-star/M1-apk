package com.m1.pricequotes

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.webkit.*
import android.widget.*
import android.text.InputType
import android.graphics.drawable.GradientDrawable

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("price_quotes", MODE_PRIVATE) }
    private var webView: WebView? = null
    private val navy = Color.rgb(8, 31, 79)
    private val blue = Color.rgb(24, 111, 220)
    private val pale = Color.rgb(243, 247, 255)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = navy
        window.navigationBarColor = navy
        val saved = prefs.getString("server_url", null)
        if (saved.isNullOrBlank()) showConnectScreen() else openM1(saved)
    }

    private fun rounded(color: Int, radius: Float): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = radius }

    private fun showConnectScreen(error: String? = null) {
        webView?.destroy()
        webView = null
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(26, 30, 26, 30)
            setBackgroundColor(pale)
        }
        val logo = TextView(this).apply {
            text = "▤"
            textSize = 48f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = rounded(navy, 28f)
            layoutParams = LinearLayout.LayoutParams(104, 104).apply { gravity = Gravity.CENTER; bottomMargin = 18 }
        }
        outer.addView(logo)
        outer.addView(TextView(this).apply {
            text = "Price Quotes"
            textSize = 28f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setTextColor(navy)
            gravity = Gravity.CENTER
        })
        outer.addView(TextView(this).apply {
            text = "Smart Quotes. Better Business."
            textSize = 14f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 5, 0, 26)
        })
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
            background = rounded(Color.WHITE, 24f)
            elevation = 5f
        }
        card.addView(TextView(this).apply {
            text = "Connect to your M1 server"
            textSize = 19f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(navy)
        })
        card.addView(TextView(this).apply {
            text = "Enter the server address provided by your M1 administrator."
            textSize = 13f
            setTextColor(Color.DKGRAY)
            setPadding(0, 8, 0, 12)
        })
        val address = EditText(this).apply {
            hint = "https://your-server.example.com:3210"
            singleLine = true
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setText(prefs.getString("server_url", ""))
            setPadding(14, 13, 14, 13)
            background = rounded(pale, 14f)
        }
        card.addView(address, LinearLayout.LayoutParams(-1, -2))
        card.addView(TextView(this).apply {
            text = "Language / اللغة"
            textSize = 13f
            setTextColor(navy)
            setPadding(0, 14, 0, 5)
        })
        val language = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, listOf("English", "العربية"))
            setSelection(if (prefs.getString("language", "en") == "ar") 1 else 0)
        }
        card.addView(language, LinearLayout.LayoutParams(-1, -2))
        card.addView(TextView(this).apply {
            text = "Appearance / المظهر"
            textSize = 13f
            setTextColor(navy)
            setPadding(0, 12, 0, 5)
        })
        val appearance = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, listOf("Light", "Dark", "System default"))
            setSelection(when (prefs.getString("theme", "light")) { "dark" -> 1; "auto" -> 2; else -> 0 })
        }
        card.addView(appearance, LinearLayout.LayoutParams(-1, -2))
        if (!error.isNullOrBlank()) card.addView(TextView(this).apply {
            text = error
            setTextColor(Color.RED)
            textSize = 13f
            setPadding(0, 8, 0, 0)
        })
        val connect = Button(this).apply {
            text = "CONNECT"
            isAllCaps = false
            textSize = 16f
            setTextColor(Color.WHITE)
            background = rounded(blue, 16f)
            setOnClickListener {
                val url = normalize(address.text.toString())
                if (url == null) {
                    Toast.makeText(this@MainActivity, "Enter a valid server URL", Toast.LENGTH_LONG).show()
                } else {
                    val lang = if (language.selectedItemPosition == 1) "ar" else "en"
                    val theme = when (appearance.selectedItemPosition) { 1 -> "dark"; 2 -> "auto"; else -> "light" }
                    prefs.edit().putString("server_url", url).putString("language", lang).putString("theme", theme).apply()
                    (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(address.windowToken, 0)
                    openM1(url)
                }
            }
        }
        card.addView(connect, LinearLayout.LayoutParams(-1, 54).apply { topMargin = 16 })
        outer.addView(card, LinearLayout.LayoutParams(-1, -2))
        outer.addView(TextView(this).apply {
            text = "Local network: http://SERVER-IP:3210\nRemote access: use a secure HTTPS or private VPN address."
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.DKGRAY)
            setPadding(8, 22, 8, 0)
        })
        setContentView(outer)
    }

    private fun normalize(raw: String): String? {
        var value = raw.trim().trimEnd('/')
        if (value.isEmpty()) return null
        if (!value.startsWith("http://") && !value.startsWith("https://")) value = "http://$value"
        return try {
            val uri = android.net.Uri.parse(value)
            if (uri.host.isNullOrBlank() || (uri.scheme != "http" && uri.scheme != "https")) null else value
        } catch (_: Exception) { null }
    }

    private fun openM1(url: String) {
        val wv = WebView(this)
        webView = wv
        wv.settings.javaScriptEnabled = true
        wv.settings.domStorageEnabled = true
        wv.settings.databaseEnabled = true
        wv.settings.allowFileAccess = false
        wv.settings.allowContentAccess = false
        wv.settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        wv.settings.setSupportZoom(false)
        wv.settings.cacheMode = WebSettings.LOAD_DEFAULT
        wv.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, loadedUrl: String) {
                val lang = prefs.getString("language", "en") ?: "en"
                val theme = prefs.getString("theme", "light") ?: "light"
                val js = "(function(){try{var l=" + org.json.JSONObject.quote(lang) + ",t=" + org.json.JSONObject.quote(theme) + ";var changed=localStorage.getItem('qm-lang')!==l||localStorage.getItem('qm-theme')!==t;if(changed){localStorage.setItem('qm-lang',l);localStorage.setItem('qm-theme',t);location.reload();return;}var h=document.documentElement;h.lang=l;h.dir=l==='ar'?'rtl':'ltr';if(t==='auto')h.removeAttribute('data-theme');else h.setAttribute('data-theme',t);}catch(e){}})();"
                view.evaluateJavascript(js, null)
            }
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val target = request.url
                val currentHost = android.net.Uri.parse(url).host
                return if (target.host == currentHost) false else {
                    startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, target))
                    true
                }
            }
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) showConnectScreen("Cannot reach M1 server. Check the address and connection.")
            }
        }
        wv.setDownloadListener { downloadUrl, _, _, _, _ ->
            startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(downloadUrl)))
        }
        setContentView(wv)
        wv.loadUrl(url)
    }

    override fun onBackPressed() {
        val wv = webView
        if (wv != null && wv.canGoBack()) wv.goBack() else if (wv != null) showConnectScreen() else super.onBackPressed()
    }

    override fun onDestroy() {
        webView?.destroy()
        webView = null
        super.onDestroy()
    }
}
