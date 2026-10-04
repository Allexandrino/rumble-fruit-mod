package com.rumblefruit.mixin;

import com.rumblefruit.ClientRpgCamera;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// cinematic ultimate camera: reposition AFTER vanilla has placed the camera
// (the ComputeCameraAngles event fires before vanilla positioning, so it loses)
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Inject(method = "setup", at = @At("RETURN"))
    private void rumblefruit$cinematic(BlockGetter level, Entity entity, boolean detached,
                                          boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        ClientRpgCamera.applyCinematic((Camera) (Object) this, entity);
    }
}
