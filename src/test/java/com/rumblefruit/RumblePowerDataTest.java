package com.rumblefruit;

import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// fruit elements: eating a fruit grants its element, a different fruit
// replaces it (blox fruits style), revoking clears everything
class RumblePowerDataTest {
    private ServerPlayer player;

    @BeforeEach
    void setUp() {
        player = new ServerPlayer();
    }

    @Test
    void grantGivesPowerWithTheFruitsElement() {
        // when the player eats the void fruit
        RumblePowerData.grant(player, 2);
        // then the power and the element stick
        assertTrue(RumblePowerData.hasPower(player));
        assertEquals(2, RumblePowerData.elementOf(player));
    }

    @Test
    void aDifferentFruitReplacesTheElement() {
        // given the inferno fruit eaten
        RumblePowerData.grant(player, 1);
        // when the frost fruit replaces it
        RumblePowerData.grant(player, 3);
        // then the new element wins
        assertTrue(RumblePowerData.hasPower(player));
        assertEquals(3, RumblePowerData.elementOf(player));
    }

    @Test
    void theSameFruitTwiceChangesNothing() {
        // given the nature fruit eaten
        RumblePowerData.grant(player, 4);
        int effects = player.effectLog.size();
        // when the same fruit is eaten again
        RumblePowerData.grant(player, 4);
        // then nothing is reapplied
        assertEquals(effects, player.effectLog.size());
        assertEquals(4, RumblePowerData.elementOf(player));
    }

    @Test
    void revokeClearsPowerAndElement() {
        // given a fruit eaten
        RumblePowerData.grant(player, 2);
        // when the power is revoked
        RumblePowerData.revoke(player);
        // then both are gone
        assertFalse(RumblePowerData.hasPower(player));
        assertEquals(0, RumblePowerData.elementOf(player));
    }

    @Test
    void plainGrantIsLightning() {
        // when the classic electro apple is eaten
        RumblePowerData.grant(player);
        // then the element is lightning
        assertTrue(RumblePowerData.hasPower(player));
        assertEquals(0, RumblePowerData.elementOf(player));
    }
}
