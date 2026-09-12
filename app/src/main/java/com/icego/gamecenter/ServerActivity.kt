package com.icego.gamecenter

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class ServerActivity : AppCompatActivity() {
    private lateinit var game: Game
    private lateinit var settings: GameSettings
    private lateinit var input: TextInputEditText
    private lateinit var inputLayout: TextInputLayout
    private lateinit var hint: TextView
    private var validationError = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        game = Game.fromId(intent.getStringExtra(Game.EXTRA_GAME)) ?: run {
            finish()
            return
        }
        settings = GameSettings(this)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        window.navigationBarColor = Color.TRANSPARENT
        setContentView(R.layout.activity_server)

        val root = findViewById<View>(R.id.server_root)
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

        findViewById<View>(R.id.server_back).setOnClickListener { finish() }
        findViewById<View>(R.id.server_cancel).setOnClickListener { finish() }
        findViewById<TextView>(R.id.server_game_title).setText(game.title)
        findViewById<TextView>(R.id.server_intro_subtitle).setText(
            if (game.defaultServer == null) R.string.edit_intro_demo_subtitle
            else R.string.edit_intro_subtitle
        )
        findViewById<TextView>(R.id.server_default_title).setText(
            if (game.defaultServer == null) R.string.clear_custom_title
            else R.string.restore_default_title
        )
        findViewById<TextView>(R.id.server_default_hint).setText(
            if (game.defaultServer == null) R.string.clear_custom_hint
            else R.string.restore_default_hint
        )

        inputLayout = findViewById(R.id.server_input_layout)
        input = findViewById(R.id.server_input)
        hint = findViewById(R.id.server_hint)
        input.hint = getString(R.string.server_hint, game.directory)
        input.setText(settings.customServerFor(game)?.url.orEmpty())
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                if (validationError) showValidState()
            }
            override fun afterTextChanged(text: Editable?) = Unit
        })
        findViewById<View>(R.id.server_save).setOnClickListener { saveServer() }
        showValidState()
    }

    private fun saveServer() {
        val value = input.text?.toString().orEmpty().trim()
        val server = GameServer.parse(value)
        if (value.isNotEmpty() && server == null) {
            showInvalidState()
            return
        }
        settings.save(game, server)
        finish()
    }

    private fun showValidState() {
        validationError = false
        inputLayout.setBoxBackgroundColor(color(R.color.background))
        inputLayout.setBoxStrokeColorStateList(ColorStateList.valueOf(color(R.color.primary)))
        input.setTextColor(color(R.color.ink))
        hint.layoutParams = hint.layoutParams.apply { height = dp(20) }
        hint.setText(R.string.server_input_hint)
        hint.setTextColor(color(R.color.muted))
    }

    private fun showInvalidState() {
        validationError = true
        inputLayout.setBoxBackgroundColor(color(R.color.error_surface))
        inputLayout.setBoxStrokeColorStateList(ColorStateList.valueOf(color(R.color.error)))
        input.setTextColor(color(R.color.error))
        hint.layoutParams = hint.layoutParams.apply { height = dp(38) }
        hint.setText(R.string.server_invalid_hint)
        hint.setTextColor(color(R.color.error))
    }

    private fun color(id: Int): Int = ContextCompat.getColor(this, id)

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
