package com.icego.gamecenter

import org.junit.Assert.*
import org.junit.Test

class GameDefaultsTest {
    @Test fun catalogKeepsStableIdsAndSeparateDirectories() {
        val expected = mapOf(
            "GOMOKU" to "wuziqi", "BLOKUS" to "blokus", "XIANGQI" to "xiangqi",
            "FEIXINGQI" to "feixingqi", "HEIBAIQI" to "heibaiqi"
        )
        assertEquals(expected.keys, Game.entries.map { it.name }.toSet())
        expected.forEach { (id, directory) ->
            val game = requireNotNull(Game.fromId(id))
            assertEquals(directory, game.directory)
        }
        assertNull(Game.fromId(null))
        assertNull(Game.fromId("UNKNOWN"))
    }

    @Test fun eachStoreDefaultUsesItsOwnConfigField() {
        val defaults = mapOf(
            Game.GOMOKU to BuildConfig.DEFAULT_GOMOKU_URL,
            Game.BLOKUS to BuildConfig.DEFAULT_BLOKUS_URL,
            Game.XIANGQI to BuildConfig.DEFAULT_XIANGQI_URL,
            Game.FEIXINGQI to BuildConfig.DEFAULT_FEIXINGQI_URL,
            Game.HEIBAIQI to BuildConfig.DEFAULT_HEIBAIQI_URL
        )
        defaults.forEach { (game, url) ->
            assertEquals(GameServer.parse(url)?.url, game.defaultServer?.url)
        }
    }

    @Test fun distributionHasExpectedDefaultsAndIdentity() {
        assertEquals(
            if (BuildConfig.IS_DEMO) "com.icego.gamecenter.demo" else "com.icego.gamecenter",
            BuildConfig.APPLICATION_ID
        )
        Game.entries.forEach { game ->
            if (BuildConfig.IS_DEMO) assertNull(game.defaultServer)
            else assertNotNull(game.defaultServer)
        }
    }
}
