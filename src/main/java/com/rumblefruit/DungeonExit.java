package com.rumblefruit;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

// when the Cube Titan dies the dungeon releases everyone inside
@EventBusSubscriber(modid = RumbleFruitMod.MOD_ID)
public class DungeonExit {

    @SubscribeEvent
    public static void onBossDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof CubeTitanEntity boss)) {
            return;
        }
        if (boss.level() instanceof ServerLevel level) {
            MeteorDungeon.releaseAll(level);
        }
    }
}
