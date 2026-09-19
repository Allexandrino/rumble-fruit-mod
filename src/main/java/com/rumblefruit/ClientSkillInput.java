package com.rumblefruit;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

// skill hotkeys: Z tap/hold variants, X/C/F fire on press; V charges while held
@EventBusSubscriber(modid = RumbleFruitMod.MOD_ID, value = Dist.CLIENT)
public class ClientSkillInput {
    private static final boolean[] prevDown = new boolean[4];
    private static boolean prevG = false;
    private static boolean prevH = false;
    private static boolean prevF = false;
    private static boolean prevR = false;
    private static boolean prevJump = false;
    private static int zPressTick = -1;
    private static boolean vDown = false;
    private static int vPressTick = 0;
    private static int tick = 0;

    // client-side cast animation state (read by EnergyHandRenderer)
    public static int lastCastTick = -100;
    public static int lastCastSkill = -1;

    // client-side skill cooldowns for the HUD: Z X C F V
    public static final int[] SKILL_COOLDOWN_TICKS = {60, 160, 240, 100, 100};
    public static final long[] lastSkillUseTick = {-1000, -1000, -1000, -1000, -1000};

    private static void notifyCast(int skillId) {
        Minecraft mc = Minecraft.getInstance();
        lastCastTick = mc.player != null ? mc.player.tickCount : 0;
        lastCastSkill = skillId;
        int idx = skillId >= 0 && skillId <= 3 ? skillId : skillId == 5 ? 4 : -1;
        if (idx >= 0) {
            lastSkillUseTick[idx] = tick;
        }
    }

    // HUD helper: remaining cooldown fraction 0..1 for skill index (0=Z,1=X,2=C,3=F,4=V)
    public static float cooldownFraction(int idx) {
        long elapsed = tick - lastSkillUseTick[idx];
        if (elapsed >= SKILL_COOLDOWN_TICKS[idx]) {
            return 0.0F;
        }
        return 1.0F - (float) elapsed / (float) SKILL_COOLDOWN_TICKS[idx];
    }

    @SubscribeEvent
    public static void onClientTickPre(ClientTickEvent.Pre event) {
        // with the fruit's power active, F toggles the wings — eat the vanilla
        // "swap hands" clicks so F never moves the item to the offhand
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.options != null && ClientPowerData.has()) {
            while (mc.options.keySwapOffhand.consumeClick()) {
                // consume and discard
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        tick++;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || ModKeyBindings.SKILL_Z == null) {
            vDown = false;
            zPressTick = -1;
            return;
        }
        boolean hasItem = ClientPowerData.has();

        // debug: log first successful item detection
        if (hasItem && tick % 100 == 0) {
            System.out.println("[rumblefruit] holding item, keybinds ready");
        }

        // Z: tap = Z1 projectile, hold 0.5s = Z2 pull+stun, hold 1.5s+ = Z3 dragon
        boolean z = ModKeyBindings.SKILL_Z.isDown();
        if (z && zPressTick < 0 && hasItem) {
            zPressTick = tick;
        }
        if (!z && zPressTick >= 0 && hasItem) {
            int held = tick - zPressTick;
            int variant = held >= 30 ? 2 : held >= 10 ? 1 : 0; // 1.5s, 0.5s, tap
            System.out.println("[rumblefruit] Z released, held=" + held + " variant=" + variant);
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(new SkillPacket(0, variant));
            notifyCast(0);
            zPressTick = -1;
        }
        if (!hasItem) {
            zPressTick = -1;
        }

        // X/C: fire on press
        net.minecraft.client.KeyMapping[] keys = {null, ModKeyBindings.SKILL_X,
                ModKeyBindings.SKILL_C};
        for (int i = 1; i < keys.length; i++) {
            boolean down = keys[i].isDown();
            if (down && !prevDown[i] && hasItem) {
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(new SkillPacket(i, 0));
                notifyCast(i);
            }
            prevDown[i] = down;
        }

        // H: cycle the combat stance (fists -> electro sword -> electro bow)
        boolean h = ModKeyBindings.STANCE.isDown();
        if (h && !prevH && hasItem) {
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(new StancePacket());
        }
        prevH = h;

        // F: toggle the lightning wings (+ exorcist mask, flight)
        boolean f = ModKeyBindings.SKILL_F.isDown();
        if (f && !prevF && hasItem) {
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(new SkillPacket(8, 0));
        }
        prevF = f;

        // R: Release — the nuke ultimate (spends the fruit)
        boolean r = ModKeyBindings.SKILL_R.isDown();
        if (r && !prevR && hasItem) {
            System.out.println("[rumblefruit] R pressed, sending release");
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(new SkillPacket(9, 0));
            notifyCast(9);
        }
        prevR = r;

        // winged flight input: tell the server when SPACE is held with the wings out
        boolean wingsOut = ClientWingsData.isActive(mc.player.getUUID());
        boolean jump = wingsOut && mc.options.keyJump.isDown();
        if (jump != prevJump) {
            prevJump = jump;
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(new WingsInputPacket(jump));
        }

        // V: hold to charge, release to fire
        boolean v = ModKeyBindings.SKILL_V.isDown();
        if (v && !vDown && hasItem) {
            vPressTick = tick;
        }
        if (!v && vDown && hasItem) {
            int held = tick - vPressTick;
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(new SkillPacket(5, SkillExecutor.chargeLevel(held)));
            notifyCast(5);
        }
        vDown = v;
    }

    public static boolean isChargingV() {
        return vDown;
    }

    public static int vHeldTicks() {
        return vDown ? tick - vPressTick : 0;
    }

    // Z currently held (charging that skill)
    public static boolean isChargingSkill() {
        return zPressTick >= 0;
    }

    public static int skillHeldTicks() {
        if (zPressTick >= 0) {
            return tick - zPressTick;
        }
        return 0;
    }
}
