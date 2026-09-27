package com.rumblefruit;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

// the Fallen Exorcist as a proper model — a colossal corrupted angel-knight:
// horned white mask with burning golden eyes, dark gold-trimmed robe with a
// flared skirt, sleeved arms, and two great feathered wings. no blocks.
public class ExorcistModel extends EntityModel<FallenExorcistEntity> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "exorcist"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart skirt;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightWing;
    private final ModelPart leftWing;

    public ExorcistModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.skirt = root.getChild("skirt");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightWing = root.getChild("right_wing");
        this.leftWing = root.getChild("left_wing");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // head: the horned exorcist mask, golden eyes burning through
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -5.0F, -2.5F, 5.0F, 5.0F, 5.0F),
                PartPose.offset(0.0F, 2.0F, 0.0F));
        head.addOrReplaceChild("horn_r",
                CubeListBuilder.create().texOffs(40, 24).addBox(-0.6F, -3.6F, -0.6F, 1.2F, 3.6F, 1.2F),
                PartPose.offsetAndRotation(1.9F, -4.6F, 0.2F, -0.35F, 0.0F, -0.5F));
        head.addOrReplaceChild("horn_l",
                CubeListBuilder.create().texOffs(40, 24).addBox(-0.6F, -3.6F, -0.6F, 1.2F, 3.6F, 1.2F),
                PartPose.offsetAndRotation(-1.9F, -4.6F, 0.2F, -0.35F, 0.0F, 0.5F));
        head.addOrReplaceChild("eye_r",
                CubeListBuilder.create().texOffs(40, 16).addBox(-0.5F, -0.6F, -0.15F, 1.0F, 1.2F, 0.3F),
                PartPose.offset(1.1F, -3.0F, -2.6F));
        head.addOrReplaceChild("eye_l",
                CubeListBuilder.create().texOffs(40, 16).addBox(-0.5F, -0.6F, -0.15F, 1.0F, 1.2F, 0.3F),
                PartPose.offset(-1.1F, -3.0F, -2.6F));

        // body: dark gold-trimmed robe
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 16).addBox(-3.5F, 0.0F, -2.5F, 7.0F, 8.0F, 5.0F),
                PartPose.offset(0.0F, 7.0F, 0.0F));
        // the flared skirt — the exorcist floats, it has no legs
        root.addOrReplaceChild("skirt",
                CubeListBuilder.create().texOffs(0, 32).addBox(-4.5F, 0.0F, -3.0F, 9.0F, 9.0F, 6.0F),
                PartPose.offset(0.0F, 14.0F, 0.0F));
        // sleeved arms
        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40, 32).addBox(-2.0F, 0.0F, -1.5F, 3.0F, 9.0F, 3.0F),
                PartPose.offset(-5.6F, 7.5F, 0.0F));
        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(40, 32).addBox(-1.0F, 0.0F, -1.5F, 3.0F, 9.0F, 3.0F),
                PartPose.offset(5.6F, 7.5F, 0.0F));

        // wings: three great feather fans each
        PartDefinition rightWing = root.addOrReplaceChild("right_wing",
                CubeListBuilder.create(), PartPose.offset(-2.5F, 4.0F, 2.2F));
        PartDefinition leftWing = root.addOrReplaceChild("left_wing",
                CubeListBuilder.create(), PartPose.offset(2.5F, 4.0F, 2.2F));
        float[][] feathers = {
                {0.6F, -1.2F, -0.75F, 9.0F},
                {1.8F, -2.2F, -0.35F, 10.5F},
                {2.8F, -3.0F, 0.10F, 11.0F},
        };
        for (int i = 0; i < feathers.length; i++) {
            float px = feathers[i][0];
            float py = feathers[i][1];
            float zRot = feathers[i][2];
            float len = feathers[i][3];
            rightWing.addOrReplaceChild("rf" + i,
                    CubeListBuilder.create().texOffs(32, 0).addBox(-len, -1.4F, -0.5F, len, 2.8F, 1.0F),
                    PartPose.offsetAndRotation(-px, py, 0.0F, 0.0F, 0.0F, -zRot));
            leftWing.addOrReplaceChild("lf" + i,
                    CubeListBuilder.create().texOffs(32, 0).addBox(0.0F, -1.4F, -0.5F, len, 2.8F, 1.0F),
                    PartPose.offsetAndRotation(px, py, 0.0F, 0.0F, 0.0F, zRot));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(FallenExorcistEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * 0.0174533F;
        head.xRot = headPitch * 0.0174533F;
        // the strike: the right arm sweeps down through the attack animation
        float swing = this.attackTime > 0.0F ? Mth.sin(this.attackTime * (float) Math.PI) : 0.0F;
        rightArm.xRot = -swing * 2.4F;
        rightArm.zRot = swing * 0.5F;
        leftArm.xRot = swing * 0.3F;
        // idle menace: arms drift, wings breathe, the skirt sways
        rightArm.xRot += Mth.sin(ageInTicks * 0.08F) * 0.06F;
        leftArm.xRot += Mth.cos(ageInTicks * 0.08F) * 0.06F;
        skirt.xRot = Mth.sin(ageInTicks * 0.06F) * 0.04F;
        float flap = Mth.sin(ageInTicks * 0.12F) * 0.12F;
        rightWing.yRot = -0.5F - flap;
        leftWing.yRot = 0.5F + flap;
    }

    @Override
    public void renderToBuffer(com.mojang.blaze3d.vertex.PoseStack poseStack,
                               com.mojang.blaze3d.vertex.VertexConsumer buffer, int packedLight,
                               int packedOverlay, int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
