package com.icego.gamecenter

import java.net.URI

/** 配置的是游戏网页入口，API 路由仍由游戏网页负责。 */
class GameServer private constructor(val url: String) {
    private val origin = URI(url)

    fun allowsNavigation(url: String): Boolean {
        val target = runCatching { URI(url) }.getOrNull() ?: return false
        return target.scheme.equals("https", ignoreCase = true) &&
            target.host.equals(origin.host, ignoreCase = true) &&
            target.rawUserInfo == null && effectivePort(target) == effectivePort(origin)
    }

    companion object {
        fun parse(value: String): GameServer? {
            val uri = runCatching { URI(value.trim()) }.getOrNull() ?: return null
            if (!uri.scheme.equals("https", ignoreCase = true) || uri.host.isNullOrBlank() ||
                uri.rawUserInfo != null || uri.rawQuery != null || uri.rawFragment != null ||
                (uri.port != -1 && uri.port !in 1..65535)) return null
            // 目录入口必须带 /，否则网页中的相对资源与 API 地址可能解析到上一级。
            val normalized = uri.toASCIIString().let { if (it.endsWith('/')) it else "$it/" }
            return GameServer(normalized)
        }

        private fun effectivePort(uri: URI): Int = if (uri.port == -1) 443 else uri.port
    }
}
