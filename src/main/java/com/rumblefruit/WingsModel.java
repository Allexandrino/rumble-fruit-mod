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
import net.minecraft.util.Mth;

// angel wings: two white feathered wings (covert + primary feather layers) on the back,
// plus the hazbin-hotel style exorcist mask (horned) rendered in head space.
public class WingsModel extends Model {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "wings"), "main");

    private final ModelPart bodyRoot;
    private final ModelPart rightWing;
    private final ModelPart leftWing;
    private final ModelPart mask;
    private final ModelPart halo;
    private final ModelPart costume;

    public WingsModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.bodyRoot = root.getChild("body");
        this.rightWing = bodyRoot.getChild("right_wing");
        this.leftWing = bodyRoot.getChild("left_wing");
        this.mask = root.getChild("mask");
        this.halo = root.getChild("halo");
        this.costume = root.getChild("costume");
    }

    // one angel wing: long primary feathers fanning from up-out to down-out,
    // a second row of secondary feathers behind them, shorter coverts on top
    private static void buildWing(PartDefinition wing, boolean mirrored) {
        // primaries: pivot along the shoulder arc, feather strip fanning downward
        float[][] primaries = {
                // px, py, zRot, length
                {0.8F, -0.8F, -0.90F, 7.0F},
                {2.0F, -1.6F, -0.55F, 8.0F},
                {2.9F, -2.5F, -0.20F, 9.0F},
                {3.4F, -3.3F, 0.15F, 9.0F},
                {3.5F, -4.0F, 0.50F, 8.0F},
                {3.4F, -4.5F, 0.85F, 7.0F},
        };
        for (int i = 0; i < primaries.length; i++) {
            float px = mirrored ? -primaries[i][0] : primaries[i][0];
            float zRot = mirrored ? -primaries[i][2] : primaries[i][2];
            float len = primaries[i][3];
            CubeListBuilder cube = CubeListBuilder.create().texOffs(0, 4);
            if (mirrored) {
                cube.addBox(-len, -1.0F, -0.60F, len, 2.0F, 0.5F);
            } else {
                cube.addBox(0.0F, -1.0F, -0.60F, len, 2.0F, 0.5F);
            }
            wing.addOrReplaceChild("p" + i, cube,
                    PartPose.offsetAndRotation(px, primaries[i][1], 0.0F, 0.0F, 0.0F, zRot));
        }
        // secondaries: a longer, fuller row behind the primaries (more wingspan + detail)
        float[][] secondaries = {
                {0.4F, -0.2F, -0.75F, 9.0F},
                {1.4F, -0.9F, -0.40F, 10.0F},
                {2.2F, -1.6F, -0.05F, 11.0F},
                {2.6F, -2.3F, 0.30F, 10.0F},
        };
        for (int i = 0; i < secondaries.length; i++) {
            float px = mirrored ? -secondaries[i][0] : secondaries[i][0];
            float zRot = mirrored ? -secondaries[i][2] : secondaries[i][2];
            float len = secondaries[i][3];
            CubeListBuilder cube = CubeListBuilder.create().texOffs(0, 8);
            if (mirrored) {
                cube.addBox(-len, -1.0F, 0.10F, len, 2.2F, 0.5F);
            } else {
                cube.addBox(0.0F, -1.0F, 0.10F, len, 2.2F, 0.5F);
            }
            wing.addOrReplaceChild("s" + i, cube,
                    PartPose.offsetAndRotation(px, secondaries[i][1], 0.15F, 0.0F, 0.0F, zRot));
        }
        // coverts: shorter layer, slightly toward the back
        float[][] coverts = {
                {0.5F, -0.5F, -0.70F, 4.0F},
                {1.5F, -1.1F, -0.35F, 4.5F},
                {2.2F, -1.8F, 0.00F, 5.0F},
                {2.6F, -2.4F, 0.35F, 4.5F},
        };
        for (int i = 0; i < coverts.length; i++) {
            float px = mirrored ? -coverts[i][0] : coverts[i][0];
            float zRot = mirrored ? -coverts[i][2] : coverts[i][2];
            float len = coverts[i][3];
            CubeListBuilder cube = CubeListBuilder.create().texOffs(0, 0);
            if (mirrored) {
                cube.addBox(-len, -0.9F, -1.05F, len, 1.8F, 0.4F);
            } else {
                cube.addBox(0.0F, -0.9F, -1.05F, len, 1.8F, 0.4F);
            }
            wing.addOrReplaceChild("c" + i, cube,
                    PartPose.offsetAndRotation(px, coverts[i][1], 0.3F, 0.0F, 0.0F, zRot));
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);

        PartDefinition right = body.addOrReplaceChild("right_wing",
                CubeListBuilder.create(), PartPose.offset(1.2F, 0.8F, 2.2F));
        buildWing(right, false);
        PartDefinition left = body.addOrReplaceChild("left_wing",
                CubeListBuilder.create(), PartPose.offset(-1.2F, 0.8F, 2.2F));
        buildWing(left, true);

        // hazbin-hotel exorcist mask: white horned mask over the face (head space)
        PartDefinition mask = root.addOrReplaceChild("mask",
                CubeListBuilder.create().texOffs(0, 20).addBox(-4.5F, -8.5F, -5.2F, 9.0F, 9.0F, 1.0F),
                PartPose.ZERO);
        mask.addOrReplaceChild("horn_r",
                CubeListBuilder.create().texOffs(32, 0).addBox(-0.9F, -4.6F, -0.9F, 1.8F, 4.6F, 1.8F),
                PartPose.offsetAndRotation(3.4F, -8.0F, -1.0F, 0.0F, 0.0F, -0.5F));
        mask.addOrReplaceChild("horn_l",
                CubeListBuilder.create().texOffs(32, 0).addBox(-0.9F, -4.6F, -0.9F, 1.8F, 4.6F, 1.8F),
                PartPose.offsetAndRotation(-3.4F, -8.0F, -1.0F, 0.0F, 0.0F, 0.5F));

        // golden halo floating above the head (head space): thin square ring with god spikes
        PartDefinition halo = root.addOrReplaceChild("halo", CubeListBuilder.create(), PartPose.offset(0.0F, -11.5F, 0.0F));
        halo.addOrReplaceChild("h_front",
                CubeListBuilder.create().texOffs(32, 16).addBox(-4.0F, -0.25F, -4.6F, 8.0F, 0.5F, 1.2F), PartPose.ZERO);
        halo.addOrReplaceChild("h_back",
                CubeListBuilder.create().texOffs(32, 16).addBox(-4.0F, -0.25F, 3.4F, 8.0F, 0.5F, 1.2F), PartPose.ZERO);
        halo.addOrReplaceChild("h_left",
                CubeListBuilder.create().texOffs(32, 18).addBox(-4.6F, -0.25F, -3.4F, 1.2F, 0.5F, 6.8F), PartPose.ZERO);
        halo.addOrReplaceChild("h_right",
                CubeListBuilder.create().texOffs(32, 18).addBox(3.4F, -0.25F, -3.4F, 1.2F, 0.5F, 6.8F), PartPose.ZERO);
        // sharp god rays on the halo corners (alastor-style drama)
        float[][] spikes = {{-4.0F, -4.0F}, {4.0F, -4.0F}, {-4.0F, 4.0F}, {4.0F, 4.0F}};
        for (int i = 0; i < spikes.length; i++) {
            halo.addOrReplaceChild("spike" + i,
                    CubeListBuilder.create().texOffs(32, 24).addBox(-0.5F, -2.6F, -0.5F, 1.0F, 2.6F, 1.0F),
                    PartPose.offset(spikes[i][0], 0.0F, spikes[i][1]));
        }

        // electro-god costume extras (body space): high collar + sharp coat tails
        // (pushed back so they clear the inflated robe shell)
        PartDefinition costume = root.addOrReplaceChild("costume", CubeListBuilder.create(), PartPose.ZERO);
        costume.addOrReplaceChild("collar",
                CubeListBuilder.create().texOffs(0, 32).addBox(-4.5F, -1.0F, 0.0F, 9.0F, 4.5F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 0.4F, 2.9F, 0.42F, 0.0F, 0.0F));
        costume.addOrReplaceChild("coat_l",
                CubeListBuilder.create().texOffs(16, 32).addBox(-4.2F, 0.0F, 0.0F, 4.0F, 9.0F, 0.6F),
                PartPose.offsetAndRotation(0.0F, 11.5F, 2.9F, 0.14F, 0.0F, 0.06F));
        costume.addOrReplaceChild("coat_r",
                CubeListBuilder.create().texOffs(24, 32).addBox(0.2F, 0.0F, 0.0F, 4.0F, 9.0F, 0.6F),
                PartPose.offsetAndRotation(0.0F, 11.5F, 2.9F, 0.14F, 0.0F, -0.06F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    // flap the wings: slow powerful beats while airborne, gentle sway on the ground
    public void setFlap(float ageInTicks, boolean flying) {
        float speed = flying ? 0.35F : 0.1F;
        float amp = flying ? 0.5F : 0.05F;
        float fold = Mth.sin(ageInTicks * speed) * amp;
        rightWing.yRot = -0.15F - fold;
        leftWing.yRot = 0.15F + fold;
        float lift = Mth.cos(ageInTicks * speed) * amp * 0.6F;
        rightWing.zRot = -lift;
        leftWing.zRot = lift;
    }

    public void renderWings(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay) {
        bodyRoot.render(poseStack, buffer, packedLight, packedOverlay);
    }

    public void renderWings(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                            int color) {
        bodyRoot.render(poseStack, buffer, packedLight, packedOverlay, color);
    }

    public void renderCostume(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                              int color) {
        costume.render(poseStack, buffer, packedLight, packedOverlay, color);
    }

    public void renderMask(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay) {
        mask.render(poseStack, buffer, packedLight, packedOverlay);
    }

    public void renderHalo(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay) {
        halo.render(poseStack, buffer, packedLight, packedOverlay);
    }

    public void renderCostume(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay) {
        costume.render(poseStack, buffer, packedLight, packedOverlay);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                               int color) {
        renderWings(poseStack, buffer, packedLight, packedOverlay);
        renderMask(poseStack, buffer, packedLight, packedOverlay);
    }
}
