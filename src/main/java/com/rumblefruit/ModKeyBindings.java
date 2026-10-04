package com.rumblefruit;

import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = RumbleFruitMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModKeyBindings {
    public static KeyMapping MAP;

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        MAP = new KeyMapping("key.rumblefruit.map", GLFW.GLFW_KEY_M, "key.categories.rumblefruit");
        event.register(MAP);
    }
}
