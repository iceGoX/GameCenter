package com.icego.gamecenter

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

// 游戏展示与默认地址的唯一映射；真实默认地址由产品变体注入。
enum class Game(
    @get:StringRes val title: Int,
    @get:StringRes val description: Int,
    @get:DrawableRes val cover: Int,
    @get:DrawableRes val badge: Int,
    val directory: String
) {
    GOMOKU(R.string.gomoku, R.string.gomoku_description, R.drawable.gomoku_cover, R.drawable.bg_pistachio, "wuziqi"),
    BLOKUS(R.string.blokus, R.string.blokus_description, R.drawable.blokus_cover, R.drawable.bg_peach, "blokus"),
    XIANGQI(R.string.xiangqi, R.string.xiangqi_description, R.drawable.xiangqi_cover, R.drawable.bg_sand, "xiangqi"),
    FEIXINGQI(R.string.feixingqi, R.string.feixingqi_description, R.drawable.feixingqi_cover, R.drawable.bg_sky, "feixingqi"),
    HEIBAIQI(R.string.heibaiqi, R.string.heibaiqi_description, R.drawable.heibaiqi_cover, R.drawable.bg_mint, "heibaiqi");

    val defaultServer: GameServer?
        get() = GameServer.parse(when (this) {
            GOMOKU -> BuildConfig.DEFAULT_GOMOKU_URL
            BLOKUS -> BuildConfig.DEFAULT_BLOKUS_URL
            XIANGQI -> BuildConfig.DEFAULT_XIANGQI_URL
            FEIXINGQI -> BuildConfig.DEFAULT_FEIXINGQI_URL
            HEIBAIQI -> BuildConfig.DEFAULT_HEIBAIQI_URL
        })

    companion object {
        const val EXTRA_GAME = "game"
        fun fromId(id: String?): Game? = entries.firstOrNull { it.name == id }
    }
}
