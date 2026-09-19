package com.lightningfruit;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// client -> server: stance combat action. action 0 = sword slash, 1 = bow release,
// 2 = fist strike (punches and kicks, charge = combo move index)
public record AttackPacket(int action, int charge) implements CustomPacketPayload {
    public static final Type<AttackPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(LightningFruitMod.MOD_ID, "attack"));
    public static final StreamCodec<FriendlyByteBuf, AttackPacket> CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeInt(p.action); buf.writeInt(p.charge); },
            buf -> new AttackPacket(buf.readInt(), buf.readInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AttackPacket packet, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof net.minecraft.server.level.ServerPlayer sender)
                    || !LightningPowerData.hasPower(sender)) {
                return;
            }
            if (packet.action == 0) {
                StanceCombat.slash(sender, packet.charge);
            } else if (packet.action == 1) {
                StanceCombat.bowRelease(sender, packet.charge);
            } else if (packet.action == 2) {
                StanceCombat.punch(sender, packet.charge);
            }
        });
    }
}
