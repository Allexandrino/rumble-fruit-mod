package com.rumblefruit;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

// the angel costume robe, built like vanilla armor: boxes inflated by 0.6px over the
// player model so the fabric never clips through the skin, even mid-animation
public class AngelRobeLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation ROBE =
            ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "textures/entity/angel_robe.png");
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "angel_robe"), "main");

    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public AngelRobeLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                          EntityModelSet modelSet) {
        super(parent);
        ModelPart root = modelSet.bakeLayer(LAYER_LOCATION);
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createRobeLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        float i = 0.6F; // armor inflation: clearance over the skin boxes
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16, 16)
                        .addBox(-4.0F - i, -i, -2.0F - i, 8.0F + 2 * i, 12.0F + 2 * i, 4.0F + 2 * i),
                PartPose.ZERO);
        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40, 16)
                        .addBox(-3.0F - i, -2.0F - i, -2.0F - i, 4.0F + 2 * i, 12.0F + 2 * i, 4.0F + 2 * i),
                PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(32, 48)
                        .addBox(-1.0F - i, -2.0F - i, -2.0F - i, 4.0F + 2 * i, 12.0F + 2 * i, 4.0F + 2 * i),
                PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 16)
                        .addBox(-2.0F - i, -i, -2.0F - i, 4.0F + 2 * i, 12.0F + 2 * i, 4.0F + 2 * i),
                PartPose.offset(-1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(16, 48)
                        .addBox(-2.0F - i, -i, -2.0F - i, 4.0F + 2 * i, 12.0F + 2 * i, 4.0F + 2 * i),
                PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!ClientWingsData.isActive(player.getUUID())) {
            return;
        }
        PlayerModel<AbstractClientPlayer> parent = this.getParentModel();
        copyPose(parent.body, body);
        copyPose(parent.rightArm, rightArm);
        copyPose(parent.leftArm, leftArm);
        copyPose(parent.rightLeg, rightLeg);
        copyPose(parent.leftLeg, leftLeg);

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(ROBE));
        body.render(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);
        rightArm.render(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);
        leftArm.render(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);
        rightLeg.render(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);
        leftLeg.render(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);
    }

    private static void copyPose(ModelPart from, ModelPart to) {
        to.x = from.x;
        to.y = from.y;
        to.z = from.z;
        to.xRot = from.xRot;
        to.yRot = from.yRot;
        to.zRot = from.zRot;
    }
}
