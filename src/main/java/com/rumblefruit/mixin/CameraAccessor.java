package com.rumblefruit.mixin;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Camera.class)
public interface CameraAccessor {
    @Accessor("position")
    @Mutable
    void rumblefruit$setPosition(Vec3 pos);

    @Accessor("detached")
    @Mutable
    void rumblefruit$setDetached(boolean detached);

    @Accessor("xRot")
    @Mutable
    void rumblefruit$setXRot(float pitch);

    @Accessor("yRot")
    @Mutable
    void rumblefruit$setYRot(float yaw);

    @Accessor("roll")
    @Mutable
    void rumblefruit$setRoll(float roll);
}
