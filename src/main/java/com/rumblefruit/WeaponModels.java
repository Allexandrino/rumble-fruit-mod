package com.rumblefruit;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

// custom 3d weapon models (own geometry + textures, rendered through the entity pipeline —
// the same one the wings use, which works on every loader). sword blade points +Y.
public class WeaponModels extends Model {
    public static final ModelLayerLocation SWORD_LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "sword"), "main");
    public static final ResourceLocation SWORD_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "textures/entity/sword_model.png");
    public static final ResourceLocation SWORD_HOLY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "textures/entity/sword_model_holy.png");

    private final ModelPart root;

    public WeaponModels(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
    }

    public static WeaponModels sword(ModelPart root) {
        return new WeaponModels(root);
    }

    // electro SPEAR: a long wrapped shaft with a leaf-bladed head, side lugs
    // and a glowing core running the full length (grip at the origin, +Y up)
    public static LayerDefinition createSwordLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // shaft: long and slim, wrapped in two bands
        root.addOrReplaceChild("shaft",
                CubeListBuilder.create().texOffs(0, 0).addBox(-0.45F, -3.0F, -0.45F, 0.9F, 20.0F, 0.9F),
                PartPose.ZERO);
        root.addOrReplaceChild("wrap_low",
                CubeListBuilder.create().texOffs(6, 0).addBox(-0.55F, -0.6F, -0.55F, 1.1F, 0.5F, 1.1F),
                PartPose.ZERO);
        root.addOrReplaceChild("wrap_mid",
                CubeListBuilder.create().texOffs(12, 0).addBox(-0.55F, 7.5F, -0.55F, 1.1F, 0.5F, 1.1F),
                PartPose.ZERO);
        // butt spike at the bottom of the shaft
        root.addOrReplaceChild("butt",
                CubeListBuilder.create().texOffs(18, 0).addBox(-0.3F, -3.9F, -0.3F, 0.6F, 1.0F, 0.6F),
                PartPose.ZERO);
        // side lugs where the head meets the shaft
        root.addOrReplaceChild("lug_l",
                CubeListBuilder.create().texOffs(0, 6).addBox(-2.0F, 16.6F, -0.5F, 1.6F, 0.7F, 1.0F),
                PartPose.ZERO);
        root.addOrReplaceChild("lug_r",
                CubeListBuilder.create().texOffs(18, 6).addBox(0.4F, 16.6F, -0.5F, 1.6F, 0.7F, 1.0F),
                PartPose.ZERO);
        // the leaf blade: wide at the base, tapering to a long point
        root.addOrReplaceChild("blade_base",
                CubeListBuilder.create().texOffs(0, 12).addBox(-1.1F, 17.0F, -0.35F, 2.2F, 3.6F, 0.7F),
                PartPose.ZERO);
        root.addOrReplaceChild("blade_mid",
                CubeListBuilder.create().texOffs(12, 12).addBox(-0.75F, 20.6F, -0.3F, 1.5F, 2.6F, 0.6F),
                PartPose.ZERO);
        root.addOrReplaceChild("blade_tip",
                CubeListBuilder.create().texOffs(24, 12).addBox(-0.4F, 23.2F, -0.25F, 0.8F, 2.2F, 0.5F),
                PartPose.ZERO);
        // the glowing core along the whole spear
        root.addOrReplaceChild("core",
                CubeListBuilder.create().texOffs(32, 12).addBox(-0.28F, 0.2F, -0.4F, 0.56F, 25.0F, 0.8F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    public void renderWeapon(PoseStack poseStack, VertexConsumer buffer, int packedLight) {
        root.render(poseStack, buffer, packedLight,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
    }

    public void renderWeapon(PoseStack poseStack, VertexConsumer buffer, int packedLight, int color) {
        root.render(poseStack, buffer, packedLight,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, color);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                               int color) {
        renderWeapon(poseStack, buffer, packedLight);
    }
}
