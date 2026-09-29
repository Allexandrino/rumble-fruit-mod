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

// screen-space impact VFX sequencer: every big moment plays a screen effect on
// a timeline (buildup -> peak -> decay). space tear for the J blast, impact
// (burning vignette + flash + grade, tinted in the fruit's color) for heavy
// casts and meteor landings, glitch bursts for the transformation, motion blur
// for the slow-mo fall, and the pencil-sketch impact frame: for a blink the
// world is redrawn as a hand-drawn sketch (casts, swirl breaks)
@EventBusSubscriber(modid = RumbleFruitMod.MOD_ID, value = Dist.CLIENT)
public class ImpactShaders {
    private static final ResourceLocation SPACETEAR =
            ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "shaders/post/spacetear.json");
    private static final ResourceLocation IMPACT =
            ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "shaders/post/impact.json");
    private static final ResourceLocation GLITCH =
            ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "shaders/post/glitch.json");
    private static final ResourceLocation SKETCH =
            ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "shaders/post/sketch.json");
    private static final ResourceLocation BLUR =
            ResourceLocation.withDefaultNamespace("shaders/post/blur.json");

    private static final long TEAR_MILLIS = 2200;
    private static final long IMPACT_MILLIS = 900;
    private static final long GLITCH_MILLIS = 600;
    private static final long SKETCH_MILLIS = 450;

    private static final int NONE = 0;
    private static final int TEAR = 1;
    private static final int IMPACT_FX = 2;
    private static final int GLITCH_FX = 3;
    private static final int SKETCH_FX = 4;

    private static int mode = NONE;
    private static long start = -1;
    private static float tintR = 0.5F;
    private static float tintG = 0.83F;
    private static float tintB = 1.0F;

    private static boolean shaderOn;
    private static boolean blurOn;
    private static long prevTicksLeft = -1;
    private static int prevCombo = -1;
    private static boolean wasHoly;

    private ImpactShaders() {
    }

    // the J blast rips space open
    public static void spaceTear() {
        mode = TEAR;
        start = System.currentTimeMillis();
    }

    // heavy casts burn the screen edges in the fruit's color
    public static void impact() {
        int color = com.rumblefruit.core.ElementCatalog.byId(ClientPowerData.element()).color();
        tintR = ((color >> 16) & 0xFF) / 255.0F;
        tintG = ((color >> 8) & 0xFF) / 255.0F;
        tintB = (color & 0xFF) / 255.0F;
        mode = IMPACT_FX;
        start = System.currentTimeMillis();
    }

    // the transformation glitches reality
    public static void glitch() {
        mode = GLITCH_FX;
        start = System.currentTimeMillis();
    }

    // the pencil-sketch impact frame: a blink of hand-drawn world
    public static void sketch() {
        // the lightest beat — never tramples a heavier moment
        if (mode != NONE && active()) {
            return;
        }
        mode = SKETCH_FX;
        start = System.currentTimeMillis();
    }

    private static long duration() {
        return switch (mode) {
            case TEAR -> TEAR_MILLIS;
            case IMPACT_FX -> IMPACT_MILLIS;
            case GLITCH_FX -> GLITCH_MILLIS;
            case SKETCH_FX -> SKETCH_MILLIS;
            default -> 0;
        };
    }

    private static boolean active() {
        return mode != NONE && start >= 0 && System.currentTimeMillis() - start < duration();
    }

    // the timeline shape: buildup -> peak -> decay
    private static float intensity(long elapsed, long dur) {
        float x = (float) elapsed / (float) dur;
        if (x < 0.2F) {
            return x / 0.2F; // buildup
        }
        if (x < 0.5F) {
            return 1.0F; // peak
        }
        return Math.max(0.0F, 1.0F - (x - 0.5F) / 0.5F); // decay
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
            impact();
        }
        prevTicksLeft = left;
        // J detonation / landing: combat-anim transitions rip the screen
        int combo = ClientCombatAnim.comboOf(mc.player.getUUID());
        if (combo != prevCombo && (combo == 20 || combo == 21)) {
            spaceTear();
        }
        prevCombo = combo;
        // F transformation: reality glitches
        boolean holy = ClientWingsData.isActive(mc.player.getUUID());
        if (holy != wasHoly) {
            glitch();
        }
        wasHoly = holy;

        boolean on = active();
        boolean blur = combo == 20;
        if (on != shaderOn || blur != blurOn) {
            shaderOn = on;
            blurOn = blur;
            refresh(mc);
        }
        if (shaderOn) {
            animate(mc);
        }
        if (!on && mode != NONE && start >= 0 && System.currentTimeMillis() - start >= duration()) {
            mode = NONE;
            start = -1;
        }
    }

    private static void refresh(Minecraft mc) {
        if (!shaderOn) {
            if (blurOn) {
                mc.gameRenderer.loadEffect(BLUR);
            } else {
                mc.gameRenderer.shutdownEffect();
            }
            return;
        }
        switch (mode) {
            case TEAR -> mc.gameRenderer.loadEffect(SPACETEAR);
            case IMPACT_FX -> mc.gameRenderer.loadEffect(IMPACT);
            case GLITCH_FX -> mc.gameRenderer.loadEffect(GLITCH);
            case SKETCH_FX -> mc.gameRenderer.loadEffect(SKETCH);
            default -> {
            }
        }
    }

    private static void animate(Minecraft mc) {
        PostChain chain = ((com.rumblefruit.mixin.GameRendererAccessor) mc.gameRenderer).rumblefruit$getPostEffect();
        if (chain == null) {
            return;
        }
        long elapsed = System.currentTimeMillis() - start;
        float seconds = elapsed / 1000.0F;
        float k = intensity(elapsed, duration());
        for (PostPass pass : ((com.rumblefruit.mixin.PostChainAccessor) chain).rumblefruit$getPasses()) {
            setFloat(pass, "Time", seconds);
            setFloat(pass, "Intensity", k);
            setFloat(pass, "Sweep", 2.0F); // full-frame sketch (no band sweep)
            Uniform center = pass.getEffect().getUniform("Center");
            if (center != null) {
                center.set(0.5F, 0.45F);
            }
            Uniform tint = pass.getEffect().getUniform("Tint");
            if (tint != null) {
                tint.set(tintR, tintG, tintB);
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
