package com.lightningfruit;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;


import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// combat stance for the lightning fruit: 0 = fists (force lightning),
// 1 = electro sword, 2 = electro bow. cycled with H, synced to all clients.
@net.neoforged.fml.common.EventBusSubscriber(modid = LightningFruitMod.MOD_ID)
public class StanceData {
    public static final int FISTS = 0;
    public static final int SWORD = 1;
    public static final int BOW = 2;

    private static final Map<UUID, Integer> STANCE = new ConcurrentHashMap<>();

    public static int get(UUID playerId) {
        return STANCE.getOrDefault(playerId, FISTS);
    }

    public static void reset(ServerPlayer player) {
        STANCE.put(player.getUUID(), FISTS);
        sync(player);
    }

    public static void cycle(ServerPlayer player) {
        int next = (get(player.getUUID()) + 1) % 3;
        STANCE.put(player.getUUID(), next);
        float pitch = next == SWORD ? 1.4F : next == BOW ? 1.0F : 1.8F;
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.6F, pitch);
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        sync(player);
    }

    private static void sync(ServerPlayer player) {
        net.neoforged.neoforge.network.PacketDistributor.sendToAllPlayers(
                new StanceSyncPacket(player.getUUID(), get(player.getUUID())));
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void onLogin(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            sync(serverPlayer);
        }
    }
}
