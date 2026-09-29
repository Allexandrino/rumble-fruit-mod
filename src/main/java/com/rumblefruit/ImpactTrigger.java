package com.rumblefruit;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

// the ult cut-in trigger: whenever a player's blow lands on a living hitbox,
// that player gets the pencil-sketch band rushing across their screen
@EventBusSubscriber(modid = RumbleFruitMod.MOD_ID)
public class ImpactTrigger {

    // shared by LivingIncomingDamageEvent and the guardian swirls (not LivingEntities)
    public static void onPlayerLandedHit(ServerPlayer player) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                player, new CombatAnimPacket(player.getUUID(), 41));
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            onPlayerLandedHit(player);
        }
    }
}
