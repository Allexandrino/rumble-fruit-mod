package com.lightningfruit;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record ChargePacket(UUID playerId, float charge) implements CustomPacketPayload {
    public static final Type<ChargePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(LightningFruitMod.MOD_ID, "charge"));
    public static final StreamCodec<FriendlyByteBuf, ChargePacket> CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeUUID(p.playerId); buf.writeFloat(p.charge); },
            buf -> new ChargePacket(buf.readUUID(), buf.readFloat()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ChargePacket packet, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientChargeData.set(packet.playerId, packet.charge));
    }
}
