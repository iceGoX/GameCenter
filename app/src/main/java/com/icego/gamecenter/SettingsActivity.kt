package com.icego.gamecenter

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

class SettingsActivity : AppCompatActivity() {
    private lateinit var settings: GameSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = GameSettings(this)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        window.navigationBarColor = Color.TRANSPARENT
        setContentView(R.layout.activity_settings)

        val root = findViewById<View>(R.id.settings_root)
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
        findViewById<View>(R.id.settings_back).setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        if (::settings.isInitialized) showServers()
    }

    private fun showServers() {
        val rows = findViewById<LinearLayout>(R.id.server_rows)
        rows.removeAllViews()
        Game.entries.forEachIndexed { index, game ->
            val row = LayoutInflater.from(this).inflate(R.layout.item_server, rows, false)
            row.findViewById<TextView>(R.id.server_game_title).setText(game.title)
            row.findViewById<TextView>(R.id.server_status).text = statusFor(game)
            row.findViewById<FrameLayout>(R.id.server_badge).setBackgroundResource(
                game.badge
            )
            row.setOnClickListener {
                startActivity(Intent(this, ServerActivity::class.java)
                    .putExtra(Game.EXTRA_GAME, game.name))
            }
            if (index == Game.entries.lastIndex) {
                row.layoutParams = (row.layoutParams as LinearLayout.LayoutParams).apply {
                    bottomMargin = 0
                }
            }
            rows.addView(row)
        }
    }

    private fun statusFor(game: Game): String = when {
        settings.customServerFor(game) != null -> getString(R.string.using_custom_server)
        game.defaultServer != null -> getString(R.string.using_default_server)
        else -> getString(R.string.server_not_configured)
    }
}
