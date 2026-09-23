package com.rumblefruit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import com.mojang.blaze3d.shaders.Uniform;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

// screen-space impact shaders: the space tear (radial rift distortion) on the
// J blast, F transformation, V cast and meteor landings, and a heavy motion
// blur during the slow-mo fall. vanilla post-chain pipeline, animated
// uniforms (time / intensity / center)
@EventBusSubscriber(modid = RumbleFruitMod.MOD_ID, value = Dist.CLIENT)
public class ImpactShaders {
    private static final ResourceLocation SPACETEAR =
            ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "shaders/post/spacetear.json");
    private static final ResourceLocation BLUR =
            ResourceLocation.withDefaultNamespace("shaders/post/blur.json");
    private static final long TEAR_MILLIS = 2200;

    private static long tearStart = -1;
    private static boolean tearOn;
    private static boolean blurOn;
    private static boolean wasFalling;
    private static boolean wasHoly;
    private static long prevTicksLeft = -1;
    private static int prevCombo = -1;

    private ImpactShaders() {
    }

    // rip the screen open
    public static void spaceTear() {
        tearStart = System.currentTimeMillis();
    }

    private static boolean tearActive() {
        return tearStart >= 0 && System.currentTimeMillis() - tearStart < TEAR_MILLIS;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        // meteor just landed: the countdown wrapped back to the full interval
        long left = ClientMeteorData.getTicksLeft();
        if (prevTicksLeft > 0 && left > prevTicksLeft) {
            spaceTear();
        }
        prevTicksLeft = left;
        // J detonation / landing: combat-anim transitions rip the screen
        int combo = ClientCombatAnim.comboOf(mc.player.getUUID());
        if (combo != prevCombo && (combo == 20 || combo == 21)) {
            spaceTear();
        }
        prevCombo = combo;
        // F transformation: wings flicker -> tear
        boolean holy = ClientWingsData.isActive(mc.player.getUUID());
        if (holy != wasHoly) {
            spaceTear();
        }
        wasHoly = holy;

        boolean tear = tearActive();
        boolean blur = combo == 20;
        if (tear != tearOn || blur != blurOn) {
            tearOn = tear;
            blurOn = blur;
            refresh(mc);
        }
        if (tearOn) {
            animate(mc);
        }
        if (!tear && tearStart >= 0 && System.currentTimeMillis() - tearStart >= TEAR_MILLIS) {
            tearStart = -1;
        }
    }

    private static void refresh(Minecraft mc) {
        if (tearOn) {
            mc.gameRenderer.loadEffect(SPACETEAR);
        } else if (blurOn) {
            mc.gameRenderer.loadEffect(BLUR);
        } else {
            mc.gameRenderer.shutdownEffect();
        }
    }

    // pump time/intensity/center into the tear every tick while it is open
    private static void animate(Minecraft mc) {
        PostChain chain = ((com.rumblefruit.mixin.GameRendererAccessor) mc.gameRenderer).rumblefruit$getPostEffect();
        if (chain == null) {
            return;
        }
        float seconds = (System.currentTimeMillis() - tearStart) / 1000.0F;
        float intensity = 1.0F - seconds / (TEAR_MILLIS / 1000.0F);
        for (PostPass pass : ((com.rumblefruit.mixin.PostChainAccessor) chain).rumblefruit$getPasses()) {
            setFloat(pass, "Time", seconds);
            setFloat(pass, "Intensity", intensity);
            Uniform center = pass.getEffect().getUniform("Center");
            if (center != null) {
                center.set(0.5F, 0.45F);
            }
        }
    }

    private static void setFloat(PostPass pass, String name, float value) {
        Uniform uniform = pass.getEffect().getUniform(name);
        if (uniform != null) {
            uniform.set(value);
        }
    }
}
