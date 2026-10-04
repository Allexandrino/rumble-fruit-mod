package com.rumblefruit;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

@EventBusSubscriber(modid = RumbleFruitMod.MOD_ID, value = Dist.CLIENT)
public class ClientChannelInput {
    private static boolean prevActive = false;

    public static boolean isChanneling() {
        return prevActive;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        // fist combat replaced the channel: LMB now throws punches and kicks
        // (see ClientStanceCombat). the channel stays off.
        if (prevActive) {
            prevActive = false;
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(new ChannelPacket(false));
        }
    }
}
