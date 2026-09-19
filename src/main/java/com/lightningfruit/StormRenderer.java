package com.lightningfruit;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

// no-op renderer: StormEntity is a server-side timer, invisible to clients
public class StormRenderer extends EntityRenderer<StormEntity> {
    public StormRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(StormEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
    }

    @Override
    public ResourceLocation getTextureLocation(StormEntity entity) {
        return null;
    }
}
