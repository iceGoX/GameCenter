package com.icego.gamecenter

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
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
        setContentView(R.layout.activity_main)

        val root = findViewById<View>(R.id.main_root)
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
        findViewById<View>(R.id.settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        if (::settings.isInitialized) showGames()
    }

    private fun showGames() {
        val configured = Game.entries.all { settings.serverFor(it) != null }
        findViewById<TextView>(R.id.home_subtitle).setText(
            if (configured) R.string.home_subtitle_ready else R.string.home_subtitle_empty
        )
        findViewById<TextView>(R.id.home_footer).setText(
            if (configured) R.string.home_footer_ready else R.string.home_footer_empty
        )

        val games = findViewById<LinearLayout>(R.id.games)
        games.removeAllViews()
        Game.entries.forEachIndexed { index, game ->
            val server = settings.serverFor(game)
            val card = LayoutInflater.from(this).inflate(R.layout.item_game, games, false)
            card.findViewById<ImageView>(R.id.game_cover).setImageResource(game.cover)
            card.findViewById<TextView>(R.id.game_title).setText(game.title)
            card.findViewById<TextView>(R.id.game_description).setText(
                if (server == null) R.string.game_not_configured else game.description
            )
            card.findViewById<TextView>(R.id.game_action_label).setText(
                if (server == null) R.string.configure_game else R.string.play_game
            )
            card.findViewById<View>(R.id.game_action).apply {
                contentDescription = getString(
                    if (server == null) R.string.configure_game else R.string.play_game
                )
                setOnClickListener {
                    if (server == null) openServerEditor(game) else openGame(game)
                }
            }
            if (index == Game.entries.lastIndex) {
                card.layoutParams = (card.layoutParams as LinearLayout.LayoutParams).apply {
                    bottomMargin = 0
                }
            }
            games.addView(card)
        }
    }

    private fun openServerEditor(game: Game) {
        startActivity(Intent(this, ServerActivity::class.java).putExtra(Game.EXTRA_GAME, game.name))
    }

    private fun openGame(game: Game) {
        startActivity(Intent(this, GameActivity::class.java).putExtra(Game.EXTRA_GAME, game.name))
    }
}
