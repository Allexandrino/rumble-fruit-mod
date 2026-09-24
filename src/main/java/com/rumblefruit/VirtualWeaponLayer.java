package com.rumblefruit;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

// renders the virtual electro sword/bow in the player's right hand in third person
// using our own 3d weapon models (same pipeline as the wings — works everywhere)
public class VirtualWeaponLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static WeaponModels swordModel = null;
    private static WeaponModels bowModel = null;

    public VirtualWeaponLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        int stance = ClientStanceData.get(player.getUUID());
        if (stance == StanceData.FISTS) {
            return;
        }
        if (swordModel == null) {
            swordModel = WeaponModels.sword(Minecraft.getInstance().getEntityModels()
                    .bakeLayer(WeaponModels.SWORD_LAYER));
            bowModel = WeaponModels.bow(Minecraft.getInstance().getEntityModels()
                    .bakeLayer(WeaponModels.BOW_LAYER));
        }
        boolean holy = ClientWingsData.isActive(player.getUUID());
        WeaponModels model = stance == StanceData.SWORD ? swordModel : bowModel;
        ResourceLocation texture = stance == StanceData.SWORD
                ? (holy ? WeaponModels.SWORD_HOLY_TEXTURE : WeaponModels.SWORD_TEXTURE)
                : (holy ? WeaponModels.BOW_HOLY_TEXTURE : WeaponModels.BOW_TEXTURE);

        poseStack.pushPose();
        this.getParentModel().rightArm.translateAndRotate(poseStack);
        if (this.getParentModel() instanceof EpicPlayerModel epic) {
            // follow the wrist through the elbow joint
            epic.rightForearm.translateAndRotate(poseStack);
        }
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90.0F));
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-25.0F)); // blade leans outboard
        poseStack.translate(1.0F / 16.0F, 0.125D, -0.55D); // grip sits in the fist
        poseStack.scale(1.3F, 1.3F, 1.3F);
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        // elemental tint: the weapon burns in the fruit's color
        var element = com.rumblefruit.core.ElementCatalog.byId(ClientPowerData.element());
        if (element.isLightning()) {
            model.renderWeapon(poseStack, vertexConsumer, packedLight);
        } else {
            model.renderWeapon(poseStack, vertexConsumer, packedLight,
                    0xFF000000 | element.color());
        }
        poseStack.popPose();
    }
}
