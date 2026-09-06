package com.icego.gamecenter

import org.junit.Assert.*
import org.junit.Test

class GameDefaultsTest {
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
