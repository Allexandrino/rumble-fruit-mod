package com.rumblefruit;

import com.rumblefruit.core.AnimCurves;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

// our own player rig: vanilla-compatible skin layout, but arms and legs are split
// at the elbow/knee so combat animations can truly bend. replaces the model inside
// the vanilla player renderer (see PlayerRendererMixin), so every layer — armor,
// wings, weapons, held items — keeps working untouched.
public class EpicPlayerModel extends PlayerModel<AbstractClientPlayer> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "epic_player"), "main");

    public final ModelPart rightForearm;
    public final ModelPart leftForearm;
    public final ModelPart rightShin;
    public final ModelPart leftShin;

    public EpicPlayerModel(ModelPart root) {
        super(root, false);
        this.rightForearm = this.rightArm.getChild("right_forearm");
        this.leftForearm = this.leftArm.getChild("left_forearm");
        this.rightShin = this.rightLeg.getChild("right_shin");
        this.leftShin = this.leftLeg.getChild("left_shin");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation quarter = new CubeDeformation(0.25F);
        CubeDeformation joint = new CubeDeformation(0.2F);

        // head
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.ZERO);
        root.addOrReplaceChild("hat",
                CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.5F)),
                PartPose.ZERO);
        root.addOrReplaceChild("ear",
                CubeListBuilder.create().texOffs(24, 0).addBox(-3.0F, -6.0F, -1.0F, 6.0F, 6.0F, 1.0F),
                PartPose.ZERO);

        // torso
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F),
                PartPose.ZERO);
        root.addOrReplaceChild("jacket",
                CubeListBuilder.create().texOffs(16, 32).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, quarter),
                PartPose.ZERO);
        root.addOrReplaceChild("cloak",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, 0.0F, -1.0F, 10.0F, 16.0F, 1.0F, new CubeDeformation(1.0F, 0.5F, 1.0F)),
                PartPose.ZERO);

        // right arm: upper (6) + forearm (6) hinged at the elbow
        PartDefinition rightArm = root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 6.0F, 4.0F),
                PartPose.offset(-5.0F, 2.0F, 0.0F));
        PartDefinition rightFore = rightArm.addOrReplaceChild("right_forearm",
                CubeListBuilder.create().texOffs(40, 22).addBox(-3.0F, -0.4F, -2.0F, 4.0F, 6.4F, 4.0F),
                PartPose.offset(0.0F, 4.0F, 0.0F));
        rightFore.addOrReplaceChild("right_forearm_sleeve",
                CubeListBuilder.create().texOffs(40, 38).addBox(-3.0F, -0.4F, -2.0F, 4.0F, 6.4F, 4.0F, joint),
                PartPose.ZERO);
        // upper sleeve lives at the root and copies the arm pose (vanilla pipeline)
        root.addOrReplaceChild("right_sleeve",
                CubeListBuilder.create().texOffs(40, 32).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 6.0F, 4.0F, quarter),
                PartPose.offset(-5.0F, 2.0F, 0.0F));

        // left arm (mirrored uv)
        PartDefinition leftArm = root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(32, 48).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 6.0F, 4.0F),
                PartPose.offset(5.0F, 2.0F, 0.0F));
        PartDefinition leftFore = leftArm.addOrReplaceChild("left_forearm",
                CubeListBuilder.create().texOffs(32, 54).mirror().addBox(-1.0F, -0.4F, -2.0F, 4.0F, 6.4F, 4.0F),
                PartPose.offset(0.0F, 4.0F, 0.0F));
        leftFore.addOrReplaceChild("left_forearm_sleeve",
                CubeListBuilder.create().texOffs(48, 54).mirror().addBox(-1.0F, -0.4F, -2.0F, 4.0F, 6.4F, 4.0F, joint),
                PartPose.ZERO);
        root.addOrReplaceChild("left_sleeve",
                CubeListBuilder.create().texOffs(48, 48).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 6.0F, 4.0F, quarter),
                PartPose.offset(5.0F, 2.0F, 0.0F));

        // right leg: thigh (6) + shin (6) hinged at the knee
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F),
                PartPose.offset(-1.9F, 12.0F, 0.0F));
        PartDefinition rightShin = rightLeg.addOrReplaceChild("right_shin",
                CubeListBuilder.create().texOffs(0, 22).addBox(-2.0F, -0.4F, -2.0F, 4.0F, 6.4F, 4.0F),
                PartPose.offset(0.0F, 6.0F, 0.0F));
        rightShin.addOrReplaceChild("right_shin_pants",
                CubeListBuilder.create().texOffs(0, 38).addBox(-2.0F, -0.4F, -2.0F, 4.0F, 6.4F, 4.0F, joint),
                PartPose.ZERO);
        root.addOrReplaceChild("right_pants",
                CubeListBuilder.create().texOffs(0, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, quarter),
                PartPose.offset(-1.9F, 12.0F, 0.0F));

        // left leg (mirrored uv)
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(16, 48).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F),
                PartPose.offset(1.9F, 12.0F, 0.0F));
        PartDefinition leftShin = leftLeg.addOrReplaceChild("left_shin",
                CubeListBuilder.create().texOffs(16, 54).mirror().addBox(-2.0F, -0.4F, -2.0F, 4.0F, 6.4F, 4.0F),
                PartPose.offset(0.0F, 6.0F, 0.0F));
        leftShin.addOrReplaceChild("left_shin_pants",
                CubeListBuilder.create().texOffs(0, 54).mirror().addBox(-2.0F, -0.4F, -2.0F, 4.0F, 6.4F, 4.0F, joint),
                PartPose.ZERO);
        root.addOrReplaceChild("left_pants",
                CubeListBuilder.create().texOffs(0, 48).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, quarter),
                PartPose.offset(1.9F, 12.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    // held items sit in the actual fist (through the elbow joint), not at the elbow
    @Override
    public void translateToHand(HumanoidArm side, com.mojang.blaze3d.vertex.PoseStack poseStack) {
        ModelPart arm = this.getArm(side);
        ModelPart fore = side == HumanoidArm.RIGHT ? this.rightForearm : this.leftForearm;
        arm.translateAndRotate(poseStack);
        fore.translateAndRotate(poseStack);
        poseStack.translate(side == HumanoidArm.RIGHT ? 0.03F : -0.03F, 0.2F, 0.0F);
    }

    // ============================ animation driver ============================

    private static final float[] REST = {-0.25F, 0.05F, 0.3F};

    @Override
    public void setupAnim(AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        // joints start neutral every frame
        rightForearm.resetPose();
        leftForearm.resetPose();
        rightShin.resetPose();
        leftShin.resetPose();

        if (player.isUsingItem()) {
            return;
        }
        float pitchRad = headPitch * 0.0174533F;

        int combo = ClientCombatAnim.comboOf(player.getUUID());
        if (combo == 9) {
            applyReleasePose(ClientCombatAnim.progressOf(player.getUUID()), ageInTicks);
            syncOverlays();
            return;
        }
        if (combo == 20) {
            applyKnockoutFall(ClientCombatAnim.progressOf(player.getUUID()));
            syncOverlays();
            return;
        }
        if (combo == 21) {
            applyKnockout(ClientCombatAnim.progressOf(player.getUUID()));
            syncOverlays();
            return;
        }
        if (combo >= 10) {
            applyFistCombo(player, combo - 10);
            syncOverlays();
            return;
        }
        if (combo >= 0) {
            applyCombo(player, combo);
            syncOverlays();
            return;
        }

        // prone flight: streamlined superman pose with slightly bent knees
        if (ClientWingsData.isActive(player.getUUID()) && !player.onGround() && !player.isInWater()) {
            rightLeg.xRot = 0.1F;
            rightLeg.zRot = 0.03F;
            leftLeg.xRot = 0.1F;
            leftLeg.zRot = -0.03F;
            rightShin.xRot = 0.15F;
            leftShin.xRot = 0.15F;
            rightArm.xRot = 0.45F;
            rightArm.yRot = 0.0F;
            rightArm.zRot = 0.45F;
            rightForearm.xRot = -0.2F;
            leftArm.xRot = 0.45F;
            leftArm.yRot = 0.0F;
            leftArm.zRot = -0.45F;
            leftForearm.xRot = -0.2F;
            syncOverlays();
            return;
        }

        if (player.swinging) {
            // vanilla arm swing, but the elbow snaps through with the strike
            rightForearm.xRot = -0.5F * Mth.sin(player.swingTime * 2.2F);
            return;
        }

        int stance = ClientStanceData.get(player.getUUID());
        // alive, not glued: breathing, subtle sway
        float breathe = Mth.sin(ageInTicks * 0.09F) * 0.035F;
        float sway = Mth.sin(ageInTicks * 0.05F) * 0.02F;
        // 1 while standing still, 0 at full stride — poses blend over the walk
        float still = 1.0F - Mth.clamp(limbSwingAmount * 2.5F, 0.0F, 1.0F);
        // realistic walk: torso counter-rotates against the legs, head stays level,
        // arms swing counter-phase, knees fold on the swing-through, whole body bobs
        float stride = Mth.cos(limbSwing * 0.6662F);
        body.yRot += -stride * limbSwingAmount * 0.16F;
        body.xRot += Math.abs(stride) * limbSwingAmount * 0.04F;
        head.yRot += stride * limbSwingAmount * 0.1F; // gaze counter-stabilized
        rightArm.xRot += stride * limbSwingAmount * 0.2F;
        leftArm.xRot += -stride * limbSwingAmount * 0.2F;
        rightShin.xRot += Math.max(0.0F, -stride) * limbSwingAmount * 1.1F;
        leftShin.xRot += Math.max(0.0F, stride) * limbSwingAmount * 1.1F;
        // vertical bob: the body rides up as a leg passes under, drops on the split
        float bob = -Math.abs(Mth.sin(limbSwing * 0.6662F)) * limbSwingAmount * 0.7F;
        head.y = bob;
        body.y = bob;
        rightArm.y = 2.0F + bob;
        leftArm.y = 2.0F + bob;
        rightLeg.y = 12.0F;
        leftLeg.y = 12.0F;
        // the epic stance: legs ALWAYS planted wide, but they keep swinging
        // with the walk — spread is a constant offset, motion is layered on top
        if (stance != StanceData.FISTS) {
            rightLeg.zRot += 0.35F;
            leftLeg.zRot += -0.35F;
            rightLeg.xRot += -0.12F * still;
            leftLeg.xRot += 0.12F * still;
        }
        if (stance == StanceData.BOW) {
            float hold = Math.max(still, 0.55F); // the bow stays raised even mid-step
            rightArm.xRot = lerp(rightArm.xRot, -1.35F + pitchRad * 0.7F + breathe * 0.5F, hold);
            rightArm.yRot = lerp(rightArm.yRot, -0.15F, hold);
            rightArm.zRot = lerp(rightArm.zRot, 0.12F, hold);
            leftArm.xRot = lerp(leftArm.xRot, -1.25F + pitchRad * 0.7F + breathe * 0.5F, hold);
            leftArm.yRot = lerp(leftArm.yRot, 0.45F, hold);
            leftArm.zRot = lerp(leftArm.zRot, -0.12F, hold);
            leftForearm.xRot = -0.2F * hold; // fingers near the string
            body.yRot = lerp(body.yRot, -0.3F + sway, hold); // side-on archer profile
        } else if (stance == StanceData.SWORD) {
            // wide swordsman stance: arms thrown clear of the body, elbows nearly
            // straight — the blade arm works from the shoulder and wrist
            float k = Math.max(still, 0.5F); // the sword arm keeps its poise mid-step
            rightArm.xRot = lerp(rightArm.xRot, -0.05F + breathe, k);
            rightArm.yRot = lerp(rightArm.yRot, -0.15F, k);
            rightArm.zRot = lerp(rightArm.zRot, 1.0F + sway, k);
            rightForearm.xRot = lerp(rightForearm.xRot, -0.12F + breathe * 0.5F, k);
            rightForearm.yRot = lerp(rightForearm.yRot, 0.3F, k); // blade outward
            leftArm.xRot = lerp(leftArm.xRot, -0.3F - breathe * 0.5F, still);
            leftArm.yRot = lerp(leftArm.yRot, 0.2F, still);
            leftArm.zRot = lerp(leftArm.zRot, -1.05F + sway, still);
            leftForearm.xRot = lerp(leftForearm.xRot, -0.15F, still);
            body.yRot = lerp(body.yRot, -0.15F + sway, still);
            body.xRot = lerp(body.xRot, 0.16F + breathe * 0.3F, still);
        } else if (still > 0.4F) {
            // fists: open boxer guard — fists up beside the chest, never crossed
            float shift = Mth.sin(ageInTicks * 0.12F) * 0.04F;
            float k = still;
            body.yRot = lerp(body.yRot, sway * 1.5F, k);
            body.xRot = lerp(body.xRot, 0.08F, k);
            rightArm.xRot = lerp(rightArm.xRot, -0.45F + breathe, k);
            rightArm.yRot = lerp(rightArm.yRot, 0.0F, k);
            rightArm.zRot = lerp(rightArm.zRot, 0.5F, k);
            rightForearm.xRot = lerp(rightForearm.xRot, -0.6F, k);
            leftArm.xRot = lerp(leftArm.xRot, -0.45F - breathe, k);
            leftArm.yRot = lerp(leftArm.yRot, 0.0F, k);
            leftArm.zRot = lerp(leftArm.zRot, -0.5F, k);
            leftForearm.xRot = lerp(leftForearm.xRot, -0.6F, k);
            rightLeg.zRot += 0.25F;
            leftLeg.zRot += -0.25F;
            rightLeg.xRot += (-0.15F + shift) * k;
            leftLeg.xRot += (0.15F - shift) * k;
        } else {
            rightLeg.zRot += 0.25F;
            leftLeg.zRot += -0.25F;
        }
        syncOverlays();
    }

    // overlay parts (hat = hair, jacket, sleeves, pants) copied the vanilla pose
    // in super.setupAnim — re-sync them to OUR final pose so they never lag behind
    private void syncOverlays() {
        hat.copyFrom(head);
        jacket.copyFrom(body);
        rightSleeve.copyFrom(rightArm);
        leftSleeve.copyFrom(leftArm);
        rightPants.copyFrom(rightLeg);
        leftPants.copyFrom(leftLeg);
    }

    // fighting-game combos with real elbows and knees:
    // 0 = double cyclone with scythe kick, 1 = rising dragon, 2 = piercing fang
    private void applyCombo(AbstractClientPlayer player, int combo) {
        float t = ClientCombatAnim.progressOf(player.getUUID());
        float windup = easeInOut(phase(t, 0.0F, 0.32F));
        float strike = easeInOut(phase(t, 0.28F, 0.55F));
        float recover = easeInOut(phase(t, 0.62F, 1.0F));
        float settle = Mth.sin(recover * (float) Math.PI) * 0.09F;

        switch (combo % 3) {
            case 0 -> { // CYCLONE
                float crouch = windup * (1.0F - strike);
                float spin = strike * Mth.TWO_PI * 2.0F;
                float extend = Math.max(windup * 0.7F, strike) * (1.0F - recover);
                // anticipation: sink low on bent knees, twist the wrong way
                body.yRot = -0.95F * crouch;
                body.xRot = 0.3F * crouch;
                rightLeg.xRot = -0.5F * crouch;
                leftLeg.xRot = -0.7F * crouch;
                rightShin.xRot = 0.9F * crouch;
                leftShin.xRot = 1.1F * crouch;
                // the whirlwind
                body.yRot += spin;
                rightArm.yRot = spin;
                body.xRot = lerp(body.xRot, -0.15F, extend) + settle;
                rightArm.xRot = lerp(REST[0], -1.6F, extend);
                rightArm.zRot = lerp(REST[2], 1.5F, extend);
                rightForearm.xRot = lerp(0.0F, -0.25F, extend); // blade arm nearly straight
                leftArm.xRot = lerp(0.05F, -1.6F, extend);
                leftArm.zRot = lerp(0.22F, -1.5F, extend);
                leftForearm.xRot = -0.35F * extend; // offhand barely tucks
                // scythe kick: knee chambers, then the shin whips out mid-spin
                float kick = Mth.sin(strike * (float) Math.PI);
                rightLeg.xRot = lerp(rightLeg.xRot, -1.4F, kick);
                rightShin.xRot = lerp(rightShin.xRot, 1.5F, kick * (1.0F - strike));
                leftLeg.xRot = lerp(leftLeg.xRot, 0.3F, kick);
            }
            case 1 -> { // RISING DRAGON
                float crouch = windup * (1.0F - strike);
                float rise = strike * (1.0F - recover);
                // deep coil: folded double, deep knee bend, fist low
                body.xRot = 0.8F * crouch;
                head.xRot += 0.4F * crouch;
                rightArm.xRot = lerp(REST[0], 0.9F, crouch);
                rightArm.zRot = lerp(REST[2], -0.4F, crouch);
                rightForearm.xRot = -0.35F * crouch;
                leftArm.xRot = 0.5F * crouch;
                leftForearm.xRot = -0.3F * crouch;
                rightLeg.xRot = -0.9F * crouch;
                leftLeg.xRot = -1.2F * crouch;
                rightShin.xRot = 1.5F * crouch;
                leftShin.xRot = 1.7F * crouch;
                // the launch: arm tears overhead, spine cracks back, legs scissor
                rightArm.xRot = lerp(rightArm.xRot, -3.1F, rise);
                rightArm.yRot = lerp(REST[1], 0.0F, rise);
                rightArm.zRot = lerp(rightArm.zRot, 0.5F, rise);
                rightForearm.xRot = lerp(rightForearm.xRot, -0.1F, rise); // full extension
                body.xRot = lerp(body.xRot, -0.5F, rise) + settle;
                head.xRot += lerp(0.4F * crouch, -0.4F, rise);
                rightLeg.xRot = lerp(rightLeg.xRot, 1.1F, rise);
                rightShin.xRot = lerp(rightShin.xRot, 0.2F, rise);
                leftLeg.xRot = lerp(leftLeg.xRot, -0.6F, rise);
                leftShin.xRot = lerp(leftShin.xRot, 0.8F, rise);
                leftArm.xRot = lerp(0.5F * crouch, 1.2F, rise);
                leftForearm.xRot = lerp(leftForearm.xRot, -0.5F, rise); // offhand fist pumps down
                leftArm.zRot = -0.5F * rise;
            }
            case 2 -> { // PIERCING FANG
                float brace = windup * (1.0F - strike);
                float lunge = strike * (1.0F - recover);
                // brace: twist away, blade hidden behind the back, knee chambered
                body.yRot = 0.95F * brace;
                body.xRot = -0.15F * brace;
                rightArm.xRot = lerp(REST[0], -0.3F, brace);
                rightArm.yRot = lerp(REST[1], -1.2F, brace);
                rightArm.zRot = lerp(REST[2], -0.5F, brace);
                rightForearm.xRot = -0.55F * brace; // elbow cocked, blade ready
                leftLeg.xRot = -0.8F * brace;
                leftShin.xRot = 1.2F * brace;
                // the lunge: full extension, body nearly horizontal, fencer's split
                body.yRot = lerp(body.yRot, -0.55F, lunge);
                body.xRot = lerp(body.xRot, 0.55F, lunge) + settle;
                rightArm.xRot = lerp(rightArm.xRot, -1.57F, lunge);
                rightArm.yRot = lerp(rightArm.yRot, 0.1F, lunge);
                rightArm.zRot = lerp(rightArm.zRot, 0.0F, lunge);
                rightForearm.xRot = lerp(rightForearm.xRot, -0.05F, lunge); // arm snaps straight
                head.yRot += -0.35F * lunge;
                rightLeg.xRot = lerp(rightLeg.xRot, 1.0F, lunge);
                rightShin.xRot = lerp(rightShin.xRot, 0.15F, lunge);
                leftLeg.xRot = lerp(leftLeg.xRot, -0.9F, lunge);
                leftShin.xRot = lerp(leftShin.xRot, 1.1F, lunge);
                leftArm.xRot = lerp(0.05F, 1.3F, lunge);
                leftForearm.xRot = -0.25F * lunge;
                leftArm.zRot = -0.5F * lunge;
            }
        }
        // chain flow: instead of snapping back to idle, every combo settles into
        // a coiled "ready" pose — the next strike winds up straight out of it
        if (recover > 0.0F) {
            float k = recover * 0.75F;
            body.yRot = lerp(body.yRot, -0.4F, k);
            body.xRot = lerp(body.xRot, 0.12F, k);
            rightArm.xRot = lerp(rightArm.xRot, -0.55F, k);
            rightArm.yRot = lerp(rightArm.yRot, -0.45F, k);
            rightArm.zRot = lerp(rightArm.zRot, 0.45F, k);
            rightForearm.xRot = lerp(rightForearm.xRot, -0.4F, k);
            leftArm.xRot = lerp(leftArm.xRot, -0.35F, k);
            leftArm.zRot = lerp(leftArm.zRot, -0.55F, k);
            rightLeg.xRot = lerp(rightLeg.xRot, -0.15F, k);
            leftLeg.xRot = lerp(leftLeg.xRot, 0.2F, k);
        }
    }

    // bare-handed fighting (fists stance, LMB): 0 = right jab, 1 = left cross,
    // 2 = rising uppercut, 3 = roundhouse kick — all crackling with electricity
    private void applyFistCombo(AbstractClientPlayer player, int move) {
        float t = ClientCombatAnim.progressOf(player.getUUID());
        float windup = easeInOut(phase(t, 0.0F, 0.3F));
        float strike = easeInOut(phase(t, 0.26F, 0.52F));
        float recover = easeInOut(phase(t, 0.58F, 1.0F));
        float settle = Mth.sin(recover * (float) Math.PI) * 0.08F;

        switch (move % 4) {
            case 0 -> { // HOOK KICK (вертушка): the body spins, the leg whips
                // around at head height — chambered knee, then full extension
                float spin = easeInOut(phase(t, 0.15F, 0.55F)) * Mth.TWO_PI; // full 360 body turn
                float extend = Math.max(windup * 0.6F, strike) * (1.0F - recover);
                body.yRot = spin;
                rightArm.yRot = spin;
                leftArm.yRot = spin;
                head.yRot += spin * 0.3F;
                rightLeg.xRot = lerp(0.0F, -1.7F, extend);
                rightShin.xRot = 1.7F * windup * (1.0F - strike) + 0.1F * strike; // chamber -> whip
                rightLeg.zRot = 0.5F * extend;
                rightArm.xRot = -0.6F * extend;
                rightArm.zRot = 0.5F * extend;
                rightForearm.xRot = -0.6F * extend;
                leftArm.xRot = -0.6F * extend;
                leftArm.zRot = -0.5F * extend;
                leftForearm.xRot = -0.6F * extend;
                leftLeg.xRot = 0.1F * extend;
            }
            case 1 -> { // CROSS: rear left hand drives through with full hip rotation
                float cross = strike * (1.0F - recover);
                body.yRot = lerp(-0.5F * windup, 0.55F, cross) + settle;
                body.xRot = 0.15F * cross;
                leftArm.xRot = lerp(-0.6F, -1.57F, cross);
                leftArm.yRot = lerp(0.0F, -0.1F, cross);
                leftArm.zRot = lerp(-0.5F, 0.0F, cross);
                leftForearm.xRot = lerp(-0.5F, -0.05F, cross);
                rightArm.xRot = -0.6F;
                rightArm.zRot = 0.45F;
                rightForearm.xRot = -0.5F;
                rightLeg.xRot = -0.3F * cross;
                leftLeg.xRot = 0.25F * cross;
            }
            case 2 -> { // UPPERCUT: drop low, then the fist rockets skyward
                float drop = windup * (1.0F - strike);
                float rise = strike * (1.0F - recover);
                body.xRot = 0.55F * drop;
                rightLeg.xRot = -0.6F * drop;
                leftLeg.xRot = -0.7F * drop;
                rightShin.xRot = 1.0F * drop;
                leftShin.xRot = 1.2F * drop;
                rightArm.xRot = lerp(-0.45F, 0.4F, drop);
                body.xRot = lerp(body.xRot, -0.35F, rise) + settle;
                body.yRot = -0.35F * rise;
                rightArm.xRot = lerp(rightArm.xRot, -2.6F, rise); // fist to the sky
                rightArm.yRot = lerp(0.0F, -0.15F, rise);
                rightForearm.xRot = lerp(rightForearm.xRot, -0.7F, rise);
                rightLeg.xRot = lerp(rightLeg.xRot, 0.4F, rise);
                leftLeg.xRot = lerp(leftLeg.xRot, 0.5F, rise);
                rightShin.xRot = lerp(rightShin.xRot, 0.1F, rise);
                leftShin.xRot = lerp(leftShin.xRot, 0.1F, rise);
                leftArm.xRot = 0.8F * rise;
                leftForearm.xRot = -0.6F * rise;
            }
            case 3 -> { // ROUNDHOUSE: the right leg scythes around at head height,
                // knee chambered then whipping straight — the reference move
                float chamber = windup * (1.0F - strike);
                float swing = strike * (1.0F - recover);
                body.yRot = lerp(-0.7F * chamber, 0.8F, swing) + settle;
                body.xRot = 0.15F * swing;
                // chamber: knee pulled up tight
                rightLeg.xRot = -1.6F * Math.max(chamber, swing);
                rightShin.xRot = 1.9F * chamber * (1.0F - swing); // folded
                rightShin.xRot += lerp(0.0F, 0.15F, swing);       // WHIP: leg snaps straight
                rightLeg.zRot = 0.35F * swing;                    // kicked out to the side
                // standing leg pivots, arms counter-swing for balance
                leftLeg.xRot = 0.1F * swing;
                rightArm.xRot = lerp(-0.45F, 0.9F, swing);
                rightArm.zRot = 0.6F * swing;
                leftArm.xRot = lerp(-0.45F, 0.9F, swing);
                leftArm.zRot = -0.6F * swing;
                head.yRot += -0.4F * swing; // eyes track the kick
            }
        }
        // punches chain the same way — settle into the coiled ready pose
        if (recover > 0.0F) {
            float k = recover * 0.75F;
            body.yRot = lerp(body.yRot, -0.3F, k);
            body.xRot = lerp(body.xRot, 0.1F, k);
            rightArm.xRot = lerp(rightArm.xRot, -0.45F, k);
            rightArm.zRot = lerp(rightArm.zRot, 0.5F, k);
            rightForearm.xRot = lerp(rightForearm.xRot, -0.6F, k);
            leftArm.xRot = lerp(leftArm.xRot, -0.45F, k);
            leftArm.zRot = lerp(leftArm.zRot, -0.5F, k);
            leftForearm.xRot = lerp(leftForearm.xRot, -0.6F, k);
            rightLeg.xRot = lerp(rightLeg.xRot, -0.15F, k);
            rightShin.xRot = lerp(rightShin.xRot, 0.0F, k);
            rightLeg.zRot = lerp(rightLeg.zRot, 0.25F, k);
            leftLeg.xRot = lerp(leftLeg.xRot, 0.15F, k);
        }
    }

    // J aftermath, mid-air: the slow-motion plunge — back-first, face to the
    // sky, arms thrown forward off the body and gently swaying as the
    // descent stretches on
    private void applyKnockoutFall(float t) {
        float k = easeInOut(Math.min(1.0F, t * 20.0F)); // snap horizontal within ~1s
        float flat = 1.57F * k;
        body.xRot = flat;
        head.xRot = flat * 0.85F + 0.25F * k;
        // arms reaching forward past the head — the signature "knocked out
        // of the sky" silhouette
        rightArm.xRot = flat - 0.55F * k;
        rightArm.zRot = 0.25F * k;
        leftArm.xRot = flat - 0.55F * k;
        leftArm.zRot = -0.25F * k;
        rightForearm.xRot = -0.1F * k;
        leftForearm.xRot = -0.1F * k;
        rightLeg.xRot = flat;
        rightLeg.zRot = 0.3F * k;
        leftLeg.xRot = flat;
        leftLeg.zRot = -0.3F * k;
        rightShin.xRot = 0.15F * k;
        leftShin.xRot = 0.15F * k;
        // slow-motion drift: the limbs sway softly while hanging in the air
        float drift = Mth.sin(t * 25.0F) * 0.08F * k;
        rightArm.zRot += drift;
        leftArm.zRot -= drift;
        rightLeg.zRot += drift * 0.5F;
        leftLeg.zRot -= drift * 0.5F;
    }

    // J aftermath, touchdown: sprawled flat on the back inside the crater,
    // and only then slowly climbs back up
    private void applyKnockout(float t) {
        // lying flat on the back, face to the sky, limbs sprawled
        float down = easeInOut(phase(t, 0.0F, 0.10F));
        float up = easeInOut(phase(t, 0.9F, 1.0F));
        float k = down * (1.0F - up);
        head.y = lerp(0.0F, 20.0F, k);
        body.y = lerp(0.0F, 20.0F, k);
        rightArm.y = lerp(2.0F, 20.0F, k);
        leftArm.y = lerp(2.0F, 20.0F, k);
        rightLeg.y = lerp(12.0F, 20.0F, k);
        leftLeg.y = lerp(12.0F, 20.0F, k);
        float flat = 1.57F * k;
        body.xRot = flat;
        head.xRot = flat * 0.85F + 0.25F * k; // face up, chin slightly off the dirt
        // arms thrown forward off the body — the epic "knocked out" sprawl
        rightArm.xRot = flat - 0.55F * k;
        rightArm.zRot = 0.55F * k;
        leftArm.xRot = flat - 0.55F * k;
        leftArm.zRot = -0.55F * k;
        rightLeg.xRot = flat;
        rightLeg.zRot = 0.3F * k;
        leftLeg.xRot = flat;
        leftLeg.zRot = -0.3F * k;
        rightForearm.xRot = -0.2F * k;
        leftForearm.xRot = -0.2F * k;
        rightShin.xRot = 0.15F * k;
        leftShin.xRot = 0.15F * k;
    }

    // R ascension: arms thrown skyward, elbows slightly bent, trembling with power
    private void applyReleasePose(float t, float ageInTicks) {
        float raise = easeInOut(phase(t, 0.0F, 0.18F));
        float lower = easeInOut(phase(t, 0.88F, 1.0F));
        float k = raise * (1.0F - lower);
        float tremble = Mth.sin(ageInTicks * 1.6F) * 0.05F * k;

        rightArm.xRot = lerp(REST[0], -2.9F, k) + tremble;
        rightArm.yRot = lerp(REST[1], -0.25F, k);
        rightArm.zRot = lerp(REST[2], 0.6F, k);
        rightForearm.xRot = lerp(0.0F, -0.2F, k) + tremble * 1.5F;
        leftArm.xRot = lerp(0.05F, -2.9F, k) - tremble;
        leftArm.yRot = lerp(0.0F, 0.25F, k);
        leftArm.zRot = lerp(0.22F, -0.6F, k);
        leftForearm.xRot = lerp(0.0F, -0.2F, k) - tremble * 1.5F;

        head.xRot += -0.45F * k;
        body.xRot = -0.12F * k;

        rightLeg.xRot = lerp(rightLeg.xRot, 0.06F, k);
        leftLeg.xRot = lerp(leftLeg.xRot, 0.06F, k);
        rightLeg.zRot = lerp(0.0F, 0.06F, k);
        leftLeg.zRot = lerp(0.0F, -0.06F, k);
        rightShin.xRot = 0.1F * k;
        leftShin.xRot = 0.1F * k;
    }

    private static float phase(float t, float start, float end) {
        return AnimCurves.phase(t, start, end);
    }

    private static float easeInOut(float k) {
        return AnimCurves.easeInOut(k);
    }

    private static float lerp(float a, float b, float k) {
        return AnimCurves.lerp(a, b, k);
    }
}
