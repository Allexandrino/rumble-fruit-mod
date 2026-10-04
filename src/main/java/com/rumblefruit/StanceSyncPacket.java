package com.rumblefruit;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record StanceSyncPacket(UUID playerId, int stance) implements CustomPacketPayload {
    public static final Type<StanceSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "stance_sync"));
    public static final StreamCodec<FriendlyByteBuf, StanceSyncPacket> CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeUUID(p.playerId); buf.writeInt(p.stance); },
            buf -> new StanceSyncPacket(buf.readUUID(), buf.readInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StanceSyncPacket packet, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientStanceData.set(packet.playerId, packet.stance));
    }
}
