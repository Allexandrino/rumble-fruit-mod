package com.rumblefruit;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// client -> server: the local player is holding SPACE (rise) / SHIFT (descend)
// while the wings are out
public record WingsInputPacket(boolean jumpHeld, boolean sneakHeld, boolean moving) implements CustomPacketPayload {
    public static final Type<WingsInputPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "wings_input"));
    public static final StreamCodec<FriendlyByteBuf, WingsInputPacket> CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeBoolean(p.jumpHeld); buf.writeBoolean(p.sneakHeld); buf.writeBoolean(p.moving); },
            buf -> new WingsInputPacket(buf.readBoolean(), buf.readBoolean(), buf.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WingsInputPacket packet, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof net.minecraft.server.level.ServerPlayer sender) {
                WingsData.setJumpHeld(sender.getUUID(), packet.jumpHeld);
                WingsData.setSneakHeld(sender.getUUID(), packet.sneakHeld);
                WingsData.setMoving(sender.getUUID(), packet.moving);
            }
        });
    }
}
