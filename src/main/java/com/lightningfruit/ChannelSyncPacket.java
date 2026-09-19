package com.lightningfruit;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record ChannelSyncPacket(UUID playerId, boolean active) implements CustomPacketPayload {
    public static final Type<ChannelSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(LightningFruitMod.MOD_ID, "channel_sync"));
    public static final StreamCodec<FriendlyByteBuf, ChannelSyncPacket> CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeUUID(p.playerId); buf.writeBoolean(p.active); },
            buf -> new ChannelSyncPacket(buf.readUUID(), buf.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ChannelSyncPacket packet, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientChannelData.set(packet.playerId, packet.active));
    }
}
