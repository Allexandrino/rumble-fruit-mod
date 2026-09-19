package com.rumblefruit;

import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

// wires up the Fallen Exorcist: attributes + spawn rules
@EventBusSubscriber(modid = RumbleFruitMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ModEntitySetup {

    @SubscribeEvent
    public static void onAttributeCreate(EntityAttributeCreationEvent event) {
        event.put(ModEntities.FALLEN_EXORCIST.get(), FallenExorcistEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void onSpawnPlacementRegister(net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent event) {
        event.register(ModEntities.FALLEN_EXORCIST.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.AND);
    }
}
