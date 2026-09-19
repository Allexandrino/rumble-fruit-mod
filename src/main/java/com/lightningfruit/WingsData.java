package com.lightningfruit;

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
@EventBusSubscriber(modid = LightningFruitMod.MOD_ID)
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
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 2.5F, 1.4F);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.9F, 1.3F);
            if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                        player.getX(), player.getY() + 1.2, player.getZ(), 60, 1.2, 1.0, 1.2, 0.12);
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
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
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                    player.getX(), player.getY() + 1.1, player.getZ(), 4, 0.5, 0.6, 0.5, 0.02);
        }
        if (player.onGround() || player.isInWater() || player.isCreative() || player.isSpectator()) {
            return;
        }
        Vec3 delta = player.getDeltaMovement();
        Vec3 look = player.getLookAngle();
        if (JUMP_HELD.contains(player.getUUID())) {
            // flap burst: a strong climb impulse on a short cooldown (feels like wing beats)
            long now = player.level().getGameTime();
            if (now - LAST_FLAP.getOrDefault(player.getUUID(), -100L) >= 8) {
                LAST_FLAP.put(player.getUUID(), now);
                player.setDeltaMovement(
                        delta.x + look.x * 0.35,
                        Math.min(delta.y + 0.55, 0.75),
                        delta.z + look.z * 0.35);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.7F, 1.5F);
            }
        } else {
            // glide (epic-flight wings style): dive to gain speed, pull up to trade it for lift
            double targetFall = look.y < -0.4 ? -0.75 : -0.32;
            double y = Math.max(delta.y, targetFall);
            double cap = look.y < -0.4 ? 2.2 : 1.2;
            double gain = 0.06 + Math.max(0.0, -look.y) * 0.06;
            double nx = delta.x + look.x * gain;
            double nz = delta.z + look.z * gain;
            double speed = Math.hypot(nx, nz);
            if (speed > cap) {
                nx *= cap / speed;
                nz *= cap / speed;
            }
            if (look.y > 0.25 && speed > 0.4) {
                y += 0.06; // pull-up converts speed into lift
            }
            player.setDeltaMovement(nx, y, nz);
        }
        player.hurtMarked = true; // sync velocity to the client
    }

    private static final Map<UUID, Long> LAST_FLAP = new ConcurrentHashMap<>();
}
