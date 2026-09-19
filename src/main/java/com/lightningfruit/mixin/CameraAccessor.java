package com.lightningfruit.mixin;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Camera.class)
public interface CameraAccessor {
    @Accessor("position")
    @Mutable
    void lightningfruit$setPosition(Vec3 pos);

    @Accessor("detached")
    @Mutable
    void lightningfruit$setDetached(boolean detached);

    @Accessor("xRot")
    @Mutable
    void lightningfruit$setXRot(float pitch);

    @Accessor("yRot")
    @Mutable
    void lightningfruit$setYRot(float yaw);

    @Accessor("roll")
    @Mutable
    void lightningfruit$setRoll(float roll);
}
