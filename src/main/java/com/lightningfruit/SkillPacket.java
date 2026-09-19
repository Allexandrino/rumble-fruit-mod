package com.lightningfruit;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// 0=Z orb, 1=X storm, 2=C pillar, 4=V press, 5=V release (charge in payload), 8=F wings
public record SkillPacket(int skillId, int charge) implements CustomPacketPayload {
    public static final Type<SkillPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(LightningFruitMod.MOD_ID, "skill"));
    public static final StreamCodec<FriendlyByteBuf, SkillPacket> CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeByte(p.skillId); buf.writeByte(p.charge); },
            buf -> new SkillPacket(buf.readByte(), buf.readByte()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SkillPacket packet, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof net.minecraft.server.level.ServerPlayer sender) {
                SkillExecutor.execute(sender, packet.skillId, packet.charge);
            }
        });
    }
}
