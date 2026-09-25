package com.rumblefruit;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;


import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// tracks whether a player has the angel wings out (toggled with F).
// winged flight (cult-of-azazel style): hold SPACE in the air to flap upward,
// release to glide — no creative-style hover. synced to all clients for the visuals.
@EventBusSubscriber(modid = RumbleFruitMod.MOD_ID)
public class WingsData {
    private static final Map<UUID, Boolean> ACTIVE = new ConcurrentHashMap<>();
    private static final java.util.Set<UUID> JUMP_HELD = ConcurrentHashMap.newKeySet();

    public static boolean isActive(UUID playerId) {
        return ACTIVE.getOrDefault(playerId, false);
    }

    public static void setJumpHeld(UUID playerId, boolean held) {
        if (held) {
            JUMP_HELD.add(playerId);
        } else {
            JUMP_HELD.remove(playerId);
        }
    }

    public static void setSneakHeld(UUID playerId, boolean held) {
        if (held) {
            SNEAK_HELD.add(playerId);
        } else {
            SNEAK_HELD.remove(playerId);
        }
    }

    public static void setMoving(UUID playerId, boolean moving) {
        if (moving) {
            MOVE_HELD.add(playerId);
        } else {
            MOVE_HELD.remove(playerId);
        }
    }

    private static final java.util.Set<UUID> SNEAK_HELD = ConcurrentHashMap.newKeySet();
    private static final java.util.Set<UUID> MOVE_HELD = ConcurrentHashMap.newKeySet();

    public static void toggle(ServerPlayer player) {
        setActive(player, !isActive(player.getUUID()));
    }

    public static void setActive(ServerPlayer player, boolean active) {
        UUID id = player.getUUID();
        ACTIVE.put(id, active);
        JUMP_HELD.remove(id);
        if (!active) {
            // fold the wings: stop any flight state (unless in creative/spectator)
            var abilities = player.getAbilities();
            if (!player.isCreative() && !player.isSpectator()) {
                abilities.mayfly = false;
                abilities.flying = false;
                player.onUpdateAbilities();
            }
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.8F, 0.8F);
        } else {
            // the angel transformation: thunderclap, totem chime, golden burst + sky bolt
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.2F, 1.2F);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.ELECTRO_BLAST.get(), SoundSource.WEATHER, 2.5F, 1.4F);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.9F, 1.3F);
            if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.sendParticles(com.rumblefruit.ModParticles.ELECTRO_GLOW.get(),
                        player.getX(), player.getY() + 1.2, player.getZ(), 60, 1.2, 1.0, 1.2, 0.12);
                serverLevel.sendParticles(com.rumblefruit.ModParticles.ELECTRO_GLOW.get(),
                        player.getX(), player.getY() + 1.2, player.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
                ElectroBolts.visualHoly(serverLevel, player.getX(), player.getY(), player.getZ());
            }
        }
        net.neoforged.neoforge.network.PacketDistributor.sendToAllPlayers(new WingsSyncPacket(id, active));
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!isActive(player.getUUID())) {
            return;
        }
        player.resetFallDistance(); // wings break any fall
        // the transformation burns the power bar; folding when it runs dry
        PowerChargeData.tickDrain(player);
        if (PowerChargeData.get(player.getUUID()) <= 0.0F) {
            setActive(player, false);
            return;
        }
        // god aura: tiny electric sparkles drifting off the electro-angel
        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel
                && player.level().getGameTime() % 3 == 0) {
            serverLevel.sendParticles(com.rumblefruit.ModParticles.ELECTRO_SPARK.get(),
                    player.getX(), player.getY() + 1.1, player.getZ(), 4, 0.5, 0.6, 0.5, 0.02);
        }
        if (player.onGround() || player.isInWater() || player.isCreative() || player.isSpectator()) {
            return;
        }
        Vec3 delta = player.getDeltaMovement();
        Vec3 look = player.getLookAngle();
        if (MOVE_HELD.contains(player.getUUID())) {
            // fast flight: the wings drive you exactly where the camera points
            double speed = 1.6;
            player.setDeltaMovement(
                    delta.x * 0.75 + look.x * speed * 0.25,
                    delta.y * 0.75 + look.y * speed * 0.25,
                    delta.z * 0.75 + look.z * speed * 0.25);
            long now = player.level().getGameTime();
            if (now - LAST_FLAP.getOrDefault(player.getUUID(), -100L) >= 12) {
                LAST_FLAP.put(player.getUUID(), now);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.5F, 1.4F);
            }
        } else if (JUMP_HELD.contains(player.getUUID())) {
            // wing climb: smooth strong lift while SPACE is held
            player.setDeltaMovement(delta.x * 0.98 + look.x * 0.02,
                    Math.min(delta.y + 0.09, 0.32), delta.z * 0.98 + look.z * 0.02);
        } else if (SNEAK_HELD.contains(player.getUUID())) {
            // fold the wings slightly: a controlled sink while SHIFT is held
            player.setDeltaMovement(delta.x * 0.98, Math.max(delta.y - 0.07, -0.45), delta.z * 0.98);
        } else {
            // HOVER: the wings hold you almost still in the air — the fall
            // damps to a feather bob, horizontal drift settles gently
            double y = delta.y * 0.55 - 0.035;
            y += Math.sin(player.level().getGameTime() * 0.15) * 0.006; // breathing of the air
            player.setDeltaMovement(delta.x * 0.92, y, delta.z * 0.92);
        }
        player.hurtMarked = true; // sync velocity to the client
    }

    private static final Map<UUID, Long> LAST_FLAP = new ConcurrentHashMap<>();
}
