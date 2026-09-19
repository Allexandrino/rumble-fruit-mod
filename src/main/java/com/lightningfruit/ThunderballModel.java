package com.lightningfruit;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class ThunderballModel extends EntityModel<ThunderballEntity> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(LightningFruitMod.MOD_ID, "thunderball"), "main");

    private static final float DEG45 = 0.7853982F;

    private final ModelPart root;

    public ThunderballModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeListBuilder cube = CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-8.0F, -8.0F, -8.0F, 16.0F, 16.0F, 16.0F);
        root.addOrReplaceChild("cube_a", cube, PartPose.ZERO);
        root.addOrReplaceChild("cube_b", cube, PartPose.rotation(DEG45, DEG45, 0.0F));
        root.addOrReplaceChild("cube_c", cube, PartPose.rotation(0.0F, DEG45, DEG45));
        root.addOrReplaceChild("cube_d", cube, PartPose.rotation(DEG45, 0.0F, DEG45));
        root.addOrReplaceChild("cube_e", cube, PartPose.rotation(0.0F, DEG45, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(ThunderballEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(com.mojang.blaze3d.vertex.PoseStack poseStack, com.mojang.blaze3d.vertex.VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
