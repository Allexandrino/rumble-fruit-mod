package com.rumblefruit;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE, RumbleFruitMod.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<ThunderballEntity>> THUNDERBALL =
            ENTITIES.register("thunderball", () -> EntityType.Builder
                    .<ThunderballEntity>of(ThunderballEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("rumblefruit:thunderball"));

    public static final DeferredHolder<EntityType<?>, EntityType<ElectroBoltEntity>> ELECTRO_BOLT =
            ENTITIES.register("electro_bolt", () -> EntityType.Builder
                    .<ElectroBoltEntity>of(ElectroBoltEntity::new, MobCategory.MISC)
                    .sized(0.6F, 0.6F)
                    .clientTrackingRange(16)
                    .updateInterval(20)
                    .noSave()
                    .build("rumblefruit:electro_bolt"));

    public static final DeferredHolder<EntityType<?>, EntityType<ElectroOrbEntity>> ELECTRO_ORB =
            ENTITIES.register("electro_orb", () -> EntityType.Builder
                    .<ElectroOrbEntity>of(ElectroOrbEntity::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("rumblefruit:electro_orb"));

    public static final DeferredHolder<EntityType<?>, EntityType<ElectroArrowEntity>> ELECTRO_ARROW =
            ENTITIES.register("electro_arrow", () -> EntityType.Builder
                    .<ElectroArrowEntity>of(ElectroArrowEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("rumblefruit:electro_arrow"));

    public static final DeferredHolder<EntityType<?>, EntityType<StormEntity>> STORM =
            ENTITIES.register("storm", () -> EntityType.Builder
                    .<StormEntity>of(StormEntity::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F)
                    .clientTrackingRange(16)
                    .updateInterval(20)
                    .noSave()
                    .build("rumblefruit:storm"));

    public static final DeferredHolder<EntityType<?>, EntityType<RumblePillarEntity>> LIGHTNING_PILLAR =
            ENTITIES.register("lightning_pillar", () -> EntityType.Builder
                    .<RumblePillarEntity>of(RumblePillarEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(16)
                    .updateInterval(20)
                    .noSave()
                    .build("rumblefruit:lightning_pillar"));

    public static final DeferredHolder<EntityType<?>, EntityType<FallenExorcistEntity>> FALLEN_EXORCIST =
            ENTITIES.register("fallen_exorcist", () -> EntityType.Builder
                    .<FallenExorcistEntity>of(FallenExorcistEntity::new, MobCategory.MONSTER)
                    .sized(0.8F, 2.0F) // SCALE 25 makes it a 20x50 colossus
                    .clientTrackingRange(16)
                    .build("rumblefruit:fallen_exorcist"));
}
