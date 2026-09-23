package com.rumblefruit;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

// when the Fallen Exorcist dies the chambers release everyone inside.
// the release runs on the next server tick: teleporting players right inside
// the death event mutates the player list mid-iteration (CME)
@EventBusSubscriber(modid = RumbleFruitMod.MOD_ID)
public class DungeonExit {
    private static ServerLevel pendingRealm;

    @SubscribeEvent
    public static void onBossDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof FallenExorcistEntity boss)) {
            return;
        }
        if (boss.level() instanceof ServerLevel level) {
            pendingRealm = level;
        }
    }

    @SubscribeEvent
    public static void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        if (pendingRealm != null) {
            MeteorDungeon.releaseAll(pendingRealm);
            pendingRealm = null;
        }
    }
}
