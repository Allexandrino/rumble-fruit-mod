package com.lightningfruit;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

// shared cast flourish animation (draw back -> sweep across -> settle)
public final class SkillAnim {
    private SkillAnim() {
    }

    public static void applyFlourish(PoseStack poseStack, float sinceCast) {
        float t = sinceCast / 14.0F;
        if (t < 0.35F) {
            float p = t / 0.35F;
            poseStack.translate(-0.05 * p, 0.12 * p, -0.05 * p);
            poseStack.mulPose(new Quaternionf().rotateZ((float) Math.toRadians(45.0F * p)));
            poseStack.mulPose(new Quaternionf().rotateX((float) Math.toRadians(-20.0F * p)));
        } else if (t < 0.7F) {
            float p = (t - 0.35F) / 0.35F;
            float sweep = Mth.sin(p * (float) Math.PI);
            poseStack.translate(0.35 * p - 0.05, 0.12 - 0.18 * p, -0.05 - 0.1 * sweep);
            poseStack.mulPose(new Quaternionf().rotateZ((float) Math.toRadians(45.0F - 70.0F * p)));
            poseStack.mulPose(new Quaternionf().rotateX((float) Math.toRadians(-20.0F + 10.0F * sweep)));
            poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(30.0F * sweep)));
        } else {
            float p = (t - 0.7F) / 0.3F;
            float back = 1.0F - p;
            poseStack.translate(0.35 * back, 0.0F, -0.05 * back);
            poseStack.mulPose(new Quaternionf().rotateZ((float) Math.toRadians(-25.0F * back)));
            poseStack.mulPose(new Quaternionf().rotateX((float) Math.toRadians(-10.0F * back)));
        }
    }
}
