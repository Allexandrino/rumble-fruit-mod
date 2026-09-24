package com.rumblefruit;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

@EventBusSubscriber(modid = RumbleFruitMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ThunderballModel.LAYER_LOCATION, ThunderballModel::createBodyLayer);
        event.registerLayerDefinition(WingsModel.LAYER_LOCATION, WingsModel::createBodyLayer);
        event.registerLayerDefinition(AngelRobeLayer.LAYER_LOCATION, AngelRobeLayer::createRobeLayer);
        event.registerLayerDefinition(WeaponModels.SWORD_LAYER, WeaponModels::createSwordLayer);
        event.registerLayerDefinition(WeaponModels.BOW_LAYER, WeaponModels::createBowLayer);
        event.registerLayerDefinition(EpicPlayerModel.LAYER, EpicPlayerModel::createLayer);
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (net.minecraft.client.resources.PlayerSkin.Model skin : event.getSkins()) {
            net.minecraft.client.renderer.entity.player.PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new WingsLayer(renderer, event.getEntityModels()));
                renderer.addLayer(new AngelRobeLayer(renderer, event.getEntityModels()));
                renderer.addLayer(new CrackedSkinLayer(renderer));
                renderer.addLayer(new VirtualWeaponLayer(renderer));
            }
        }
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.THUNDERBALL.get(), ThunderballRenderer::new);
        event.registerEntityRenderer(ModEntities.ELECTRO_BOLT.get(), ElectroBoltRenderer::new);
        event.registerEntityRenderer(ModEntities.ELECTRO_ORB.get(),
                net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.STORM.get(), StormRenderer::new);
        event.registerEntityRenderer(ModEntities.LIGHTNING_PILLAR.get(), RumblePillarRenderer::new);
        event.registerEntityRenderer(ModEntities.ELECTRO_ARROW.get(), ElectroArrowRenderer::new);
        event.registerEntityRenderer(ModEntities.FALLEN_EXORCIST.get(), FallenExorcistRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticleFactories(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.ELECTRO_SLASH.get(),
                sprites -> (type, level, x, y, z, yaw, pitch, roll) ->
                        new ElectroSlashParticle(level, x, y, z, yaw, pitch, roll, sprites));
        // simple glow quads: sparks of every element + glow + cloud
        event.registerSpriteSet(ModParticles.ELECTRO_SPARK.get(),
                sprites -> (type, level, x, y, z, dx, dy, dz) ->
                        ElectroSparkParticle.spark(level, x, y, z, dx, dy, dz, sprites));
        event.registerSpriteSet(ModParticles.INFERNO_SPARK.get(),
                sprites -> (type, level, x, y, z, dx, dy, dz) ->
                        ElectroSparkParticle.spark(level, x, y, z, dx, dy, dz, sprites));
        event.registerSpriteSet(ModParticles.VOID_SPARK.get(),
                sprites -> (type, level, x, y, z, dx, dy, dz) ->
                        ElectroSparkParticle.spark(level, x, y, z, dx, dy, dz, sprites));
        event.registerSpriteSet(ModParticles.FROST_SPARK.get(),
                sprites -> (type, level, x, y, z, dx, dy, dz) ->
                        ElectroSparkParticle.spark(level, x, y, z, dx, dy, dz, sprites));
        event.registerSpriteSet(ModParticles.NATURE_SPARK.get(),
                sprites -> (type, level, x, y, z, dx, dy, dz) ->
                        ElectroSparkParticle.spark(level, x, y, z, dx, dy, dz, sprites));
        event.registerSpriteSet(ModParticles.ELECTRO_GLOW.get(),
                sprites -> (type, level, x, y, z, dx, dy, dz) ->
                        ElectroSparkParticle.glow(level, x, y, z, dx, dy, dz, sprites));
        event.registerSpriteSet(ModParticles.ELECTRO_CLOUD.get(),
                sprites -> (type, level, x, y, z, dx, dy, dz) ->
                        ElectroSparkParticle.cloud(level, x, y, z, dx, dy, dz, sprites));
    }
}
