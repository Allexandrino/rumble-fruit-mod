package com.lightningfruit;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// client -> server: cycle the combat stance (H key)
public record StancePacket() implements CustomPacketPayload {
    public static final Type<StancePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(LightningFruitMod.MOD_ID, "stance"));
    public static final StreamCodec<FriendlyByteBuf, StancePacket> CODEC = StreamCodec.of(
            (buf, p) -> { },
            buf -> new StancePacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StancePacket packet, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof net.minecraft.server.level.ServerPlayer sender
                    && LightningPowerData.hasPower(sender)) {
                StanceData.cycle(sender);
            }
        });
    }
}
