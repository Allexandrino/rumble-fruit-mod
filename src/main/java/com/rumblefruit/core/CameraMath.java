package com.rumblefruit.core;

// cinematic camera math (minecraft conventions): which yaw/pitch points a
// camera at `delta = target - cameraPos`
public final class CameraMath {
    private CameraMath() {
    }

    // minecraft yaw: 0 faces +Z (south); result in degrees
    public static float lookYaw(Vec delta) {
        return (float) Math.toDegrees(Math.atan2(-delta.x(), delta.z()));
    }

    // minecraft pitch: positive looks down; result in degrees
    public static float lookPitch(Vec delta) {
        double flat = Math.sqrt(delta.x() * delta.x() + delta.z() * delta.z());
        return (float) Math.toDegrees(-Math.atan2(delta.y(), flat));
    }

    // banking roll from sideways speed (action-rpg chase cam), clamped
    public static float bankRoll(Vec velocity, float playerYawDeg) {
        double yawRad = Math.toRadians(playerYawDeg);
        double lateral = -velocity.x() * Math.cos(yawRad) - velocity.z() * Math.sin(yawRad);
        return AnimCurves.clamp((float) lateral * 14.0F, -7.0F, 7.0F);
    }

    // exponential smoothing toward a target, frame-rate independent enough for us
    public static Vec glide(Vec current, Vec target, double factor) {
        return current.lerp(target, factor);
    }
}
