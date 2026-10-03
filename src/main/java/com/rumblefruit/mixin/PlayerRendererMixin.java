package com.rumblefruit.mixin;

import com.rumblefruit.EpicPlayerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// swaps the vanilla player model for our articulated rig (elbows + knees)
// inside the stock renderer, so all layers (armor, wings, weapons) keep working.
// NOTE: the rig is baked directly from its own LayerDefinition instead of the
// shared EntityModelSet — Player Animator's dispatcher mixin can construct
// player renderers before layer definitions are registered, and the
// EntityModelSet lookup then crashes the game with "No model for layer"
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void rumblefruit$epicModel(EntityRendererProvider.Context context, boolean slim, CallbackInfo ci) {
        ((LivingEntityRendererAccessor) this).rumblefruit$setModel(
                new EpicPlayerModel(EpicPlayerModel.createLayer().bakeRoot()));
    }
}
