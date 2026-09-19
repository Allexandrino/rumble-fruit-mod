package com.lightningfruit;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;

// the user's own skin, bundled with the mod and force-applied to the local player
// (offline launchers never fetch one, so we ship it ourselves)
public class CustomSkinTexture {
    public static final ResourceLocation LOCATION =
            ResourceLocation.fromNamespaceAndPath(LightningFruitMod.MOD_ID, "player_skin_custom");
    private static boolean ready = false;

    public static void ensureRegistered() {
        if (ready) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        try (InputStream in = mc.getResourceManager().open(
                ResourceLocation.fromNamespaceAndPath(LightningFruitMod.MOD_ID, "textures/player_skin.png"))) {
            mc.getTextureManager().register(LOCATION, new DynamicTexture(NativeImage.read(in)));
            ready = true;
        } catch (Exception e) {
            System.out.println("[lightningfruit] failed to load bundled skin: " + e.getMessage());
        }
    }

    public static boolean isReady() {
        return ready;
    }
}
