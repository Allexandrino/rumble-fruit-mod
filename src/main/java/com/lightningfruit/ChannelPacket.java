package com.lightningfruit;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ChannelPacket(boolean active) implements CustomPacketPayload {
    public static final Type<ChannelPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(LightningFruitMod.MOD_ID, "channel"));
    public static final StreamCodec<FriendlyByteBuf, ChannelPacket> CODEC = StreamCodec.of(
            (buf, p) -> buf.writeBoolean(p.active),
            buf -> new ChannelPacket(buf.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ChannelPacket packet, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof net.minecraft.server.level.ServerPlayer sender) {
                LightningChannel.setActive(sender, packet.active);
            }
        });
    }
}
