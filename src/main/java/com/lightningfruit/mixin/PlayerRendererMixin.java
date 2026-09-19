package com.lightningfruit.mixin;

import com.lightningfruit.EpicPlayerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// swaps the vanilla player model for our articulated rig (elbows + knees)
// inside the stock renderer, so all layers (armor, wings, weapons) keep working
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void lightningfruit$epicModel(EntityRendererProvider.Context context, boolean slim, CallbackInfo ci) {
        ((LivingEntityRendererAccessor) this).lightningfruit$setModel(
                new EpicPlayerModel(context.bakeLayer(EpicPlayerModel.LAYER)));
    }
}
