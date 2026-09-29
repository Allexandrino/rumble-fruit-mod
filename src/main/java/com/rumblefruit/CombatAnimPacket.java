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
            // combo 40: a guardian swirl just shattered — pencil impact frame
            if (packet.combo == 40) {
                ImpactShaders.sketch();
            }
            // combo 41: your blow connected — the ult cut-in band rushes by
            if (packet.combo == 41) {
                ImpactShaders.cutin();
            }
            ClientCombatAnim.slash(packet.playerId, packet.combo);
        });
    }
}
