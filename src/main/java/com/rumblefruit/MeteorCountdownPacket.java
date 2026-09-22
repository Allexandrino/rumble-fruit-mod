package com.rumblefruit;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// server → client: ticks left until the next meteor falls (for the HUD countdown)
public record MeteorCountdownPacket(long ticksLeft) implements CustomPacketPayload {
    public static final Type<MeteorCountdownPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "meteor_countdown"));
    public static final StreamCodec<FriendlyByteBuf, MeteorCountdownPacket> CODEC = StreamCodec.of(
            (buf, p) -> buf.writeLong(p.ticksLeft),
            buf -> new MeteorCountdownPacket(buf.readLong()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MeteorCountdownPacket packet, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientMeteorData.setTicksLeft(packet.ticksLeft));
    }
}
