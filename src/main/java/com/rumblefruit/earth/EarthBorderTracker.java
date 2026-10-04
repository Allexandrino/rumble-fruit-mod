package com.rumblefruit.earth;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// watches players in the earth dimension: crossing a real country border
// fires a big title ("Вы вошли на территорию: Россия"), leaving every border
// behind announces international waters; approaching a known city or
// landmark whispers its name in the action bar
@EventBusSubscriber(modid = com.rumblefruit.RumbleFruitMod.MOD_ID)
public class EarthBorderTracker {
    public static final ResourceKey<Level> EARTH = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(com.rumblefruit.RumbleFruitMod.MOD_ID, "earth"));

    private static final Map<UUID, String> LAST_COUNTRY = new ConcurrentHashMap<>();
    private static final Map<UUID, String> LAST_PLACE = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % 20 != 0) {
            return;
        }
        if (!player.level().dimension().equals(EARTH)) {
            LAST_COUNTRY.remove(player.getUUID());
            LAST_PLACE.remove(player.getUUID());
            return;
        }
        int x = player.blockPosition().getX();
        int z = player.blockPosition().getZ();

        String country = EarthData.countryAt(x, z, true);
        String last = LAST_COUNTRY.get(player.getUUID());
        if (country == null ? last != null : !country.equals(last)) {
            // ConcurrentHashMap rejects null values — drop the key instead
            if (country == null) {
                LAST_COUNTRY.remove(player.getUUID());
            } else {
                LAST_COUNTRY.put(player.getUUID(), country);
            }
            if (country != null) {
                player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
                player.connection.send(new ClientboundSetSubtitleTextPacket(
                        Component.translatable("rumblefruit.earth_entered").withStyle(ChatFormatting.GRAY)));
                player.connection.send(new ClientboundSetTitleTextPacket(
                        Component.literal(country).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
            } else if (last != null) {
                player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 15));
                player.connection.send(new ClientboundSetSubtitleTextPacket(Component.empty()));
                player.connection.send(new ClientboundSetTitleTextPacket(
                        Component.translatable("rumblefruit.earth_waters").withStyle(ChatFormatting.BLUE)));
            }
        }

        EarthData.Place place = EarthData.nearestPlace(x, z, 320.0);
        String placeId = place == null ? null : place.id();
        String lastPlace = LAST_PLACE.get(player.getUUID());
        if (placeId == null ? lastPlace != null : !placeId.equals(lastPlace)) {
            // same null-value rule here: remove instead of putting null
            if (placeId == null) {
                LAST_PLACE.remove(player.getUUID());
            } else {
                LAST_PLACE.put(player.getUUID(), placeId);
            }
            if (place != null) {
                player.displayClientMessage(Component.translatable("rumblefruit.earth_near",
                        Component.literal(place.nameRu()).withStyle(ChatFormatting.YELLOW))
                        .withStyle(ChatFormatting.GRAY), true);
            }
        }
    }
}
