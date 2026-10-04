package com.rumblefruit;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PowerSyncPacket(boolean hasPower, int element) implements CustomPacketPayload {
    public static final Type<PowerSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "power_sync"));
    public static final StreamCodec<FriendlyByteBuf, PowerSyncPacket> CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeBoolean(p.hasPower); buf.writeInt(p.element); },
            buf -> new PowerSyncPacket(buf.readBoolean(), buf.readInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PowerSyncPacket packet, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientPowerData.set(packet.hasPower, packet.element));
    }
}
