package com.lightningfruit;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record OrbSyncPacket(UUID playerId, int orbs) implements CustomPacketPayload {
    public static final Type<OrbSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(LightningFruitMod.MOD_ID, "orb_sync"));
    public static final StreamCodec<FriendlyByteBuf, OrbSyncPacket> CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeUUID(p.playerId); buf.writeInt(p.orbs); },
            buf -> new OrbSyncPacket(buf.readUUID(), buf.readInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OrbSyncPacket packet, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientOrbData.set(packet.playerId, packet.orbs));
    }
}
