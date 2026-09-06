package com.icego.gamecenter

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible

class GameActivity : AppCompatActivity() {
    private lateinit var game: Game
    private lateinit var server: GameServer
    private lateinit var webView: WebView
    private lateinit var loadingState: View
    private lateinit var errorState: View
    private lateinit var loadingProgress: ProgressBar
    private var loadingAnimator: ObjectAnimator? = null
    private var loadFailed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        game = Game.fromId(intent.getStringExtra(Game.EXTRA_GAME)) ?: run {
            finish()
            return
        }
        server = GameSettings(this).serverFor(game) ?: run {
            finish()
            return
        }
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        window.navigationBarColor = Color.TRANSPARENT
        setContentView(R.layout.activity_game)

        val root = findViewById<View>(R.id.game_root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                maxOf(systemBars.bottom, ime.bottom)
            )
            insets
        }

        findViewById<TextView>(R.id.game_title).setText(game.title)
        findViewById<View>(R.id.game_back).setOnClickListener { confirmExit() }
        findViewById<View>(R.id.retry).setOnClickListener { webView.loadUrl(server.url) }
        findViewById<View>(R.id.error_return).setOnClickListener { confirmExit() }
        loadingState = findViewById(R.id.loading_state)
        errorState = findViewById(R.id.error_state)
        loadingProgress = findViewById(R.id.loading_progress)
        webView = findViewById(R.id.web_view)
        configureWebView()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!loadFailed && webView.canGoBack()) webView.goBack() else confirmExit()
            }
        })

        // 屏幕旋转由 Activity 保留 WebView；进程重建只恢复浏览历史，不承诺恢复内存中的对局。
        val webState = savedInstanceState?.getBundle(WEB_STATE)
        if (webState == null || webView.restoreState(webState) == null) webView.loadUrl(server.url)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                loadingProgress.progress = newProgress
                if (!loadFailed && newProgress < 100 && !loadingState.isVisible) showLoading()
            }
        }
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                if (server.allowsNavigation(uri.toString())) return false
                // HTTPS 重定向与网页链接留在应用内；非 Web 协议继续拦截。
                if (request.isForMainFrame &&
                    (uri.scheme.equals("https", ignoreCase = true) ||
                        uri.scheme.equals("http", ignoreCase = true))
                ) return false
                return true
            }

            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                loadFailed = false
                showLoading()
                loadingProgress.progress = 0
            }

            override fun onPageFinished(view: WebView, url: String?) {
                if (loadFailed) return
                loadFailed = false
                loadingAnimator?.cancel()
                loadingState.isVisible = false
                errorState.isVisible = false
                webView.isVisible = true
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) showLoadError()
            }

            override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                if (request.isForMainFrame) showLoadError()
            }
        }
    }

    private fun showLoading() {
        loadFailed = false
        webView.isVisible = false
        errorState.isVisible = false
        loadingState.isVisible = true
        loadingAnimator?.cancel()
        loadingAnimator = ObjectAnimator.ofFloat(
            findViewById(R.id.loading_icon),
            View.ROTATION,
            0f,
            360f
        ).apply {
            duration = 900L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }
    }

    private fun showLoadError() {
        loadFailed = true
        loadingAnimator?.cancel()
        webView.isVisible = false
        loadingState.isVisible = false
        errorState.isVisible = true
    }

    private fun confirmExit() {
        val content = layoutInflater.inflate(R.layout.dialog_exit, null)
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(content)
        dialog.setCanceledOnTouchOutside(true)
        content.findViewById<View>(R.id.exit_stay).setOnClickListener { dialog.dismiss() }
        content.findViewById<View>(R.id.exit_confirm).setOnClickListener {
            dialog.dismiss()
            finish()
        }
        dialog.setOnShowListener {
            val width = minOf(dp(342), resources.displayMetrics.widthPixels - dp(48))
            dialog.window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                attributes = attributes.apply { dimAmount = 0.32f }
                setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
            }
        }
        dialog.show()
        val width = minOf(dp(342), resources.displayMetrics.widthPixels - dp(48))
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            attributes = attributes.apply { dimAmount = 0.32f }
            setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onResume() {
        super.onResume()
        if (::webView.isInitialized) webView.onResume()
    }

    override fun onPause() {
        if (::webView.isInitialized) webView.onPause()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (::webView.isInitialized) {
            val webState = Bundle()
            webView.saveState(webState)
            outState.putBundle(WEB_STATE, webState)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        loadingAnimator?.cancel()
        if (::webView.isInitialized) {
            webView.stopLoading()
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.webChromeClient = null
            webView.destroy()
        }
        super.onDestroy()
    }

    private companion object {
        const val WEB_STATE = "web_state"
    }
}
