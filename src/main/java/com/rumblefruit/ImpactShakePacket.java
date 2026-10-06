package com.rumblefruit;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

// server -> all clients: the world just got HIT at (x,y,z). every player
// close enough feels the impact in the third-person camera — hard shake at
// ground zero fading to nothing at 40 blocks, plus an fov punch up close.
public record ImpactShakePacket(double x, double y, double z, float strength)
        implements CustomPacketPayload {
    public static final Type<ImpactShakePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "impact_shake"));
    public static final StreamCodec<FriendlyByteBuf, ImpactShakePacket> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeDouble(p.x);
                buf.writeDouble(p.y);
                buf.writeDouble(p.z);
                buf.writeFloat(p.strength);
            },
            buf -> new ImpactShakePacket(buf.readDouble(), buf.readDouble(), buf.readDouble(),
                    buf.readFloat()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ImpactShakePacket packet,
                              net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                return;
            }
            double dist = mc.player.position().distanceTo(new Vec3(packet.x, packet.y, packet.z));
            if (dist > 40.0) {
                return;
            }
            float shake = packet.strength * (float) (1.0 - dist / 40.0);
            ClientRpgCamera.addShake(shake);
            if (dist < 22.0) {
                ClientRpgCamera.impactPulse();
            }
        });
    }
}
