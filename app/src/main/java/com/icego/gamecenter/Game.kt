package com.icego.gamecenter

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

// 游戏展示与默认地址的唯一映射；真实默认地址由产品变体注入。
enum class Game(
    @get:StringRes val title: Int,
    @get:StringRes val description: Int,
    @get:DrawableRes val cover: Int
) {
    GOMOKU(R.string.gomoku, R.string.gomoku_description, R.drawable.gomoku_cover),
    BLOKUS(R.string.blokus, R.string.blokus_description, R.drawable.blokus_cover);

    val defaultServer: GameServer?
        get() = GameServer.parse(when (this) {
            GOMOKU -> BuildConfig.DEFAULT_GOMOKU_URL
            BLOKUS -> BuildConfig.DEFAULT_BLOKUS_URL
        })

    companion object {
        const val EXTRA_GAME = "game"
        fun fromId(id: String?): Game? = entries.firstOrNull { it.name == id }
    }
}
