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

// the Fallen Exorcist: a colossal horned FACE with four wings, looming out of
// a mound of living darkness — no body, no throne. the face bobs and breathes
// over the abyss, tendrils droop from its chin into the dark, and when it
// strikes the whole visage lunges at the prey
public class ExorcistModel extends EntityModel<FallenExorcistEntity> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "exorcist"), "main");

    private final ModelPart root;
    private final ModelPart face;
    private final ModelPart[] wings = new ModelPart[4];
    private final ModelPart[] tendrils = new ModelPart[4];

    public ExorcistModel(ModelPart root) {
        this.root = root;
        this.face = root.getChild("face");
        for (int i = 0; i < 4; i++) {
            wings[i] = root.getChild("wing" + i);
            tendrils[i] = root.getChild("tendril" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // the abyss: a bulge of darkness the face rises from, cracked with
        // light; its front edge is flush with the face so it never hides it
        root.addOrReplaceChild("mound",
                CubeListBuilder.create().texOffs(0, 24).addBox(-6.0F, 0.0F, -2.0F, 12.0F, 8.0F, 8.0F),
                PartPose.ZERO);
        root.addOrReplaceChild("mound_top",
                CubeListBuilder.create().texOffs(0, 24).addBox(-4.0F, 0.0F, -1.5F, 8.0F, 3.0F, 6.0F),
                PartPose.offset(0.0F, 8.0F, 0.0F));

        // the face: a giant horned bone mask looming out of the mound
        PartDefinition face = root.addOrReplaceChild("face",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -5.0F, -1.0F, 8.0F, 10.0F, 2.0F),
                PartPose.offset(0.0F, 13.0F, -1.0F));
        // burning eyes (this renderer does no vanilla flip: +y is UP)
        face.addOrReplaceChild("eye_r",
                CubeListBuilder.create().texOffs(32, 0).addBox(-0.9F, -0.9F, -0.3F, 1.8F, 1.8F, 0.6F),
                PartPose.offset(1.8F, 1.2F, -1.3F));
        face.addOrReplaceChild("eye_l",
                CubeListBuilder.create().texOffs(32, 0).addBox(-0.9F, -0.9F, -0.3F, 1.8F, 1.8F, 0.6F),
                PartPose.offset(-1.8F, 1.2F, -1.3F));
        // the third eye on the forehead
        face.addOrReplaceChild("eye_third",
                CubeListBuilder.create().texOffs(48, 0).addBox(-0.6F, -0.6F, -0.25F, 1.2F, 1.2F, 0.5F),
                PartPose.offset(0.0F, 3.4F, -1.25F));
        // great horns curving up out of the brow
        face.addOrReplaceChild("horn_r",
                CubeListBuilder.create().texOffs(24, 0).addBox(-0.75F, 0.0F, -0.75F, 1.5F, 6.0F, 1.5F),
                PartPose.offsetAndRotation(3.2F, 4.4F, 0.0F, 0.0F, 0.0F, -0.45F));
        face.addOrReplaceChild("horn_l",
                CubeListBuilder.create().texOffs(24, 0).addBox(-0.75F, 0.0F, -0.75F, 1.5F, 6.0F, 1.5F),
                PartPose.offsetAndRotation(-3.2F, 4.4F, 0.0F, 0.0F, 0.0F, 0.45F));

        // four vast wings spread out of the dark behind the face
        float[][] wingSpec = {
                {-3.2F, 19.0F, 1.2F, 0.55F}, {3.2F, 19.0F, 1.2F, -0.55F},
                {-2.8F, 13.5F, 1.4F, 0.30F}, {2.8F, 13.5F, 1.4F, -0.30F},
        };
        for (int i = 0; i < 4; i++) {
            boolean left = i % 2 == 0;
            PartDefinition wing = root.addOrReplaceChild("wing" + i,
                    CubeListBuilder.create(), PartPose.offset(wingSpec[i][0], wingSpec[i][1], wingSpec[i][2]));
            for (int f = 0; f < 3; f++) {
                float len = 10.0F + f * 2.0F;
                float zRot = wingSpec[i][3] + f * 0.22F * (left ? 1.0F : -1.0F);
                wing.addOrReplaceChild("f" + f,
                        CubeListBuilder.create().texOffs(0, 16)
                                .addBox(left ? -len : 0.0F, -1.5F, -0.5F, len, 3.0F, 1.0F),
                        PartPose.offsetAndRotation(0.0F, f * 0.6F, f * 0.4F, 0.0F, 0.0F, zRot));
            }
        }

        // tendrils droop from the chin into the abyss
        for (int i = 0; i < 4; i++) {
            float x = (i - 1.5F) * 1.8F;
            root.addOrReplaceChild("tendril" + i,
                    CubeListBuilder.create().texOffs(40, 16).addBox(-0.5F, -7.0F, -0.5F, 1.0F, 7.0F, 1.0F),
                    PartPose.offsetAndRotation(x, 8.0F, -1.2F, 0.12F * (i - 1.5F), 0.0F, 0.08F * (i - 1.5F)));
        }

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(FallenExorcistEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // idle menace: the face bobs over the abyss, tilting as it watches;
        // wings stir slowly; tendrils sway like kelp in a black current
        face.y = 13.0F + Mth.sin(ageInTicks * 0.05F) * 0.6F;
        face.xRot = Mth.sin(ageInTicks * 0.04F) * 0.06F;
        face.yRot = netHeadYaw * 0.015F;
        float stir = Mth.sin(ageInTicks * 0.08F) * 0.08F;
        for (int i = 0; i < 4; i++) {
            wings[i].zRot = (i % 2 == 0 ? 1 : -1) * stir;
            tendrils[i].xRot = 0.12F * (i - 1.5F) + Mth.sin(ageInTicks * 0.09F + i) * 0.07F;
        }
        // the strike: the whole visage LUNGES forward out of the dark
        if (this.attackTime > 0.0F) {
            float lunge = Mth.sin(this.attackTime * (float) Math.PI);
            face.z = -1.0F - lunge * 2.2F;
            face.xRot += lunge * 0.25F;
        } else {
            face.z = -1.0F;
        }
    }

    @Override
    public void renderToBuffer(com.mojang.blaze3d.vertex.PoseStack poseStack,
                               com.mojang.blaze3d.vertex.VertexConsumer buffer, int packedLight,
                               int packedOverlay, int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
