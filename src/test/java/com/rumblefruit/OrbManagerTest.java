package com.rumblefruit;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// OrbManager running on a fake server player — no game needed
class OrbManagerTest {
    private ServerPlayer player;

    @BeforeEach
    void setUp() {
        player = new ServerPlayer();
        player.level().setGameTime(0);
    }

    @Test
    void fullPoolByDefault() {
        assertEquals(OrbManager.MAX_ORBS, OrbManager.getOrbs(player.getUUID()));
    }

    @Test
    void consumeReducesOrbs() {
        assertEquals(2, OrbManager.consume(player, 2));
        assertEquals(OrbManager.MAX_ORBS - 2, OrbManager.getOrbs(player.getUUID()));
    }

    @Test
    void consumeCapsAtPool() {
        assertEquals(4, OrbManager.consume(player, 10));
        assertEquals(0, OrbManager.consume(player, 1));
    }

    @Test
    void tickEventRechargesAfterDelay() {
        OrbManager.consume(player, 4);
        player.level().setGameTime(99);
        OrbManager.onPlayerTick(new PlayerTickEvent.Post(player));
        assertEquals(0, OrbManager.getOrbs(player.getUUID())); // too early
        player.level().setGameTime(100);
        OrbManager.onPlayerTick(new PlayerTickEvent.Post(player));
        assertEquals(1, OrbManager.getOrbs(player.getUUID())); // recharged one
    }

    @Test
    void consumeNotifiesClients() {
        // given a clean packet log
        net.neoforged.neoforge.network.PacketDistributor.clear();
        // when orbs are consumed
        OrbManager.consume(player, 1);
        // then clients are told about the new count
        assertEquals(1, net.neoforged.neoforge.network.PacketDistributor.SENT.size());
    }

    @Test
    void rechargeNotifiesClients() {
        // given a spent pool and a clean packet log
        OrbManager.consume(player, 2);
        net.neoforged.neoforge.network.PacketDistributor.clear();
        // when the recharge ticks through the event handler
        player.level().setGameTime(100);
        OrbManager.onPlayerTick(new PlayerTickEvent.Post(player));
        // then clients learn about the recharged orb
        assertEquals(1, net.neoforged.neoforge.network.PacketDistributor.SENT.size());
        assertEquals(OrbManager.MAX_ORBS - 1, OrbManager.getOrbs(player.getUUID()));
    }

    @Test
    void tickEventIgnoresNonPlayers() {
        player.level().setGameTime(500);
        // the event always carries a Player, but only ServerPlayers get recharges
        OrbManager.onPlayerTick(new PlayerTickEvent.Post(new net.minecraft.world.entity.player.Player()));
        assertEquals(OrbManager.MAX_ORBS, OrbManager.getOrbs(player.getUUID()));
    }

    @Test
    void logoutDropsThePool() {
        // given a half-spent pool
        OrbManager.consume(player, 2);
        assertEquals(OrbManager.MAX_ORBS - 2, OrbManager.getOrbs(player.getUUID()));
        // when the player logs out
        OrbManager.onLogout(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(player));
        // then the pool is gone — a fresh pool starts full
        assertEquals(OrbManager.MAX_ORBS, OrbManager.getOrbs(player.getUUID()));
    }
}
