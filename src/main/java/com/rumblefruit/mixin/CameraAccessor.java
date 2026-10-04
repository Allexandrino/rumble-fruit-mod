package com.rumblefruit.mixin;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface CameraAccessor {
    @Accessor("position")
    @Mutable
    void rumblefruit$setPosition(Vec3 pos);

    @Accessor("detached")
    @Mutable
    void rumblefruit$setDetached(boolean detached);

    @Invoker("setRotation(FFF)V")
    void rumblefruit$setRotation(float yaw, float pitch, float roll);
}
