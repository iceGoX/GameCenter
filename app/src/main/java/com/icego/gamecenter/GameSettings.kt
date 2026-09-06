package com.icego.gamecenter

import android.content.Context
import androidx.core.content.edit

/** 自定义地址优先；清除自定义地址后回退到当前产品变体的默认地址。 */
class GameSettings(context: Context) {
    private val preferences = context.getSharedPreferences("game_servers", Context.MODE_PRIVATE)

    fun serverFor(game: Game): GameServer? =
        customServerFor(game) ?: game.defaultServer

    fun customServerFor(game: Game): GameServer? =
        preferences.getString(game.name, null)?.let(GameServer::parse)

    fun save(game: Game, server: GameServer?) {
        preferences.edit {
            if (server == null) remove(game.name) else putString(game.name, server.url)
        }
    }
}
