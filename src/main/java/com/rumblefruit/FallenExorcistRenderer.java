package com.rumblefruit;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

// renders the Fallen Exorcist as a dark humanoid with glowing golden eyes
public class FallenExorcistRenderer
        extends HumanoidMobRenderer<FallenExorcistEntity, HumanoidModel<FallenExorcistEntity>> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "textures/entity/fallen_exorcist.png");

    public FallenExorcistRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(FallenExorcistEntity entity) {
        return TEXTURE;
    }
}
