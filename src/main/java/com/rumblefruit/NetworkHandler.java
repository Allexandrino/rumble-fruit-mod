package com.rumblefruit;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = RumbleFruitMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class NetworkHandler {
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1");
        r.playToServer(ChannelPacket.TYPE, ChannelPacket.CODEC, ChannelPacket::handle);
        r.playToServer(SkillPacket.TYPE, SkillPacket.CODEC, SkillPacket::handle);
        r.playToServer(WingsInputPacket.TYPE, WingsInputPacket.CODEC, WingsInputPacket::handle);
        r.playToServer(StancePacket.TYPE, StancePacket.CODEC, StancePacket::handle);
        r.playToServer(AttackPacket.TYPE, AttackPacket.CODEC, AttackPacket::handle);
        r.playToClient(OrbSyncPacket.TYPE, OrbSyncPacket.CODEC, OrbSyncPacket::handle);
        r.playToClient(WingsSyncPacket.TYPE, WingsSyncPacket.CODEC, WingsSyncPacket::handle);
        r.playToClient(PowerSyncPacket.TYPE, PowerSyncPacket.CODEC, PowerSyncPacket::handle);
        r.playToClient(StanceSyncPacket.TYPE, StanceSyncPacket.CODEC, StanceSyncPacket::handle);
        r.playToClient(CombatAnimPacket.TYPE, CombatAnimPacket.CODEC, CombatAnimPacket::handle);
        r.playToClient(ChargePacket.TYPE, ChargePacket.CODEC, ChargePacket::handle);
        r.playToClient(ChannelSyncPacket.TYPE, ChannelSyncPacket.CODEC, ChannelSyncPacket::handle);
    }
}
