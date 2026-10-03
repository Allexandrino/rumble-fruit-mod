package com.rumblefruit;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record CombatAnimPacket(UUID playerId, int combo) implements CustomPacketPayload {
    public static final Type<CombatAnimPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "combat_anim"));
    public static final StreamCodec<FriendlyByteBuf, CombatAnimPacket> CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeUUID(p.playerId); buf.writeInt(p.combo); },
            buf -> new CombatAnimPacket(buf.readUUID(), buf.readInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CombatAnimPacket packet, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            // combo 40: a guardian swirl just shattered — the flipbook answers
            if (packet.combo == 40) {
                PencilAnimOverlay.play();
            }
            ClientCombatAnim.slash(packet.playerId, packet.combo);
            // full-body keyframed animation via Player Animator
            PlayerSkillAnimations.play(packet.playerId, packet.combo);
        });
    }
}
