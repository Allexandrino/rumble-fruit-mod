package com.rumblefruit;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

// GeckoLib model wrapper for the Fallen Exorcist: geometry lives in
// assets/rumblefruit/geo/exorcist.geo.json, animations in
// assets/rumblefruit/animations/exorcist.animation.json (procedural Molang
// curves: breathing bob, wing stir, tendril sway, core pulse, attack lunge)
public class GeckoExorcistModel extends GeoModel<FallenExorcistEntity> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "geo/exorcist.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "textures/entity/exorcist.png");
    private static final ResourceLocation ANIMATIONS =
            ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "animations/exorcist.animation.json");

    @Override
    public ResourceLocation getModelResource(FallenExorcistEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(FallenExorcistEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(FallenExorcistEntity animatable) {
        return ANIMATIONS;
    }
}
