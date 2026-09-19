package net.neoforged.neoforge.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

// vacuum fake of neoforge's PacketDistributor — records what would be sent
public class PacketDistributor {
    public static final List<Object> SENT = new ArrayList<>();

    public static void sendToAllPlayers(CustomPacketPayload packet, CustomPacketPayload... rest) {
        SENT.add(packet);
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload packet,
                                    CustomPacketPayload... rest) {
        SENT.add(packet);
    }

    public static void sendToServer(CustomPacketPayload packet, CustomPacketPayload... rest) {
        SENT.add(packet);
    }

    public static void clear() {
        SENT.clear();
    }
}
