package com.icego.gamecenter

import org.junit.Assert.*
import org.junit.Test

class GameServerTest {
    @Test fun normalizesDirectoryAndSupportsIndependentServers() {
        assertEquals("https://games.example.com/wuziqi/", GameServer.parse(" https://games.example.com/wuziqi ")?.url)
        assertEquals("https://other.example.com:8443/", GameServer.parse("https://other.example.com:8443")?.url)
    }

    @Test fun rejectsUnsupportedAndAmbiguousAddresses() {
        listOf("", "games.example.com", "http://games.example.com/", "file:///game/",
            "javascript:alert(1)", "https://user:pass@games.example.com/", "https://games.example.com/?room=1",
            "https://games.example.com/#board", "https://games.example.com:0/", "https://games.example.com:65536/",
            "https://games.example.com/with space/").forEach { assertNull(it, GameServer.parse(it)) }
    }

    @Test fun navigationIsBoundToConfiguredOrigin() {
        val server = requireNotNull(GameServer.parse("https://games.example.com/wuziqi/"))
        assertTrue(server.allowsNavigation("https://GAMES.example.com:443/wuziqi/?room=123456"))
        assertTrue(server.allowsNavigation("https://games.example.com/blokus/"))
        listOf("http://games.example.com/", "https://games.example.com:8443/",
            "https://games.example.com.evil.test/", "https://games.example.com@evil.test/",
            "https://user@games.example.com/", "file:///game/", "not a url").forEach {
            assertFalse(it, server.allowsNavigation(it))
        }
    }
}
