package com.lightningfruit.mixin;

import com.lightningfruit.CustomSkinTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// swaps the local player's skin for the one bundled with the mod
@Mixin(AbstractClientPlayer.class)
public abstract class PlayerSkinMixin {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void lightningfruit$customSkin(CallbackInfoReturnable<PlayerSkin> cir) {
        Minecraft mc = Minecraft.getInstance();
        if ((Object) this != mc.player) {
            return; // only the local user, never other players
        }
        CustomSkinTexture.ensureRegistered();
        if (!CustomSkinTexture.isReady()) {
            return;
        }
        PlayerSkin old = cir.getReturnValue();
        cir.setReturnValue(new PlayerSkin(CustomSkinTexture.LOCATION, null,
                old.capeTexture(), old.elytraTexture(), old.model(), false));
    }
}
