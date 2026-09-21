package com.rumblefruit;

import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = RumbleFruitMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModKeyBindings {
    public static KeyMapping SKILL_Z;
    public static KeyMapping SKILL_X;
    public static KeyMapping SKILL_C;
    public static KeyMapping SKILL_F;
    public static KeyMapping SKILL_V;
    public static KeyMapping SKILL_R;
    public static KeyMapping STANCE;

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        SKILL_Z = new KeyMapping("key.rumblefruit.skill_z", GLFW.GLFW_KEY_Z, "key.categories.rumblefruit");
        SKILL_X = new KeyMapping("key.rumblefruit.skill_x", GLFW.GLFW_KEY_X, "key.categories.rumblefruit");
        SKILL_C = new KeyMapping("key.rumblefruit.skill_c", GLFW.GLFW_KEY_C, "key.categories.rumblefruit");
        SKILL_F = new KeyMapping("key.rumblefruit.skill_f", GLFW.GLFW_KEY_F, "key.categories.rumblefruit");
        SKILL_V = new KeyMapping("key.rumblefruit.skill_v", GLFW.GLFW_KEY_V, "key.categories.rumblefruit");
        SKILL_R = new KeyMapping("key.rumblefruit.skill_j", GLFW.GLFW_KEY_J, "key.categories.rumblefruit");
        STANCE = new KeyMapping("key.rumblefruit.stance", GLFW.GLFW_KEY_H, "key.categories.rumblefruit");
        event.register(SKILL_Z);
        event.register(SKILL_X);
        event.register(SKILL_C);
        event.register(SKILL_F);
        event.register(SKILL_V);
        event.register(SKILL_R);
        event.register(STANCE);
        System.out.println("[rumblefruit] KeyBindings registered: Z/X/C/F/V/H");
    }
}
