package com.rumblefruit;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record WingsSyncPacket(UUID playerId, boolean active) implements CustomPacketPayload {
    public static final Type<WingsSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "wings_sync"));
    public static final StreamCodec<FriendlyByteBuf, WingsSyncPacket> CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeUUID(p.playerId); buf.writeBoolean(p.active); },
            buf -> new WingsSyncPacket(buf.readUUID(), buf.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WingsSyncPacket packet, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientWingsData.set(packet.playerId, packet.active));
    }
}
