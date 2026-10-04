package com.rumblefruit;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

// force-lightning channel: while LMB is held with the item, two spark beams hit a cone in front
@EventBusSubscriber(modid = RumbleFruitMod.MOD_ID)
public class RumbleChannel {
    private static final double RANGE = 14.0;
    private static final double CONE_DOT = 0.819; // cos(35°)
    private static final int DAMAGE_INTERVAL = 4;
    private static final int SOUND_INTERVAL = 10;
    private static final int BOLT_INTERVAL = 20;

    private static final Set<UUID> ACTIVE = new HashSet<>();
    private static final Random RANDOM = new Random();
    private static int tickCounter = 0;

    public static void setActive(ServerPlayer player, boolean active) {
        if (active) {
            ACTIVE.add(player.getUUID());
            // synthetic "using item" state: vanilla syncs it, so third-person shows
            // the channel pose (arms raised) to everyone
            player.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
        } else {
            ACTIVE.remove(player.getUUID());
            if (player.isUsingItem()) {
                player.stopUsingItem();
            }
        }
        net.neoforged.neoforge.network.PacketDistributor.sendToAllPlayers(
                new ChannelSyncPacket(player.getUUID(), active));
    }

    public static void setActive(UUID playerId, boolean active) {
        // kept for simple removals (logout); no use-state side effects here
        if (active) {
            ACTIVE.add(playerId);
        } else {
            ACTIVE.remove(playerId);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ACTIVE.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player && ACTIVE.contains(player.getUUID())) {
            boolean bareHanded = player.getMainHandItem().isEmpty();
            if (!player.isAlive() || !bareHanded || !RumblePowerData.hasPower(player)) {
                setActive(player, false);
                return;
            }
            tickCounter++;
            tickPlayer(player, (ServerLevel) player.level());
        }
    }

    private static void tickPlayer(ServerPlayer player, ServerLevel level) {
        if (tickCounter % DAMAGE_INTERVAL != 0) {
            return;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 view = player.getViewVector(1.0F);
        Vec3 reach = eye.add(view.scale(RANGE));
        BlockHitResult hit = level.clip(new ClipContext(
                eye, reach, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 target = hit.getType() == HitResult.Type.MISS ? reach : hit.getLocation();

        // cone damage
        AABB area = player.getBoundingBox().inflate(RANGE);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && e.isAlive())) {
            Vec3 to = entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0).subtract(eye);
            double dist = to.length();
            if (dist > RANGE || dist < 0.01) {
                continue;
            }
            if (view.dot(to.normalize()) > CONE_DOT) {
                entity.hurt(level.damageSources().indirectMagic(player, player), 8.0F);
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 15, 1));
            }
        }

        // impact sparks; the jagged 3d bolts from the hands are drawn client-side
        // (ChannelBoltRenderer) from the synced channel state
        level.sendParticles(ModParticles.ELECTRO_SPARK.get(),
                target.x, target.y, target.z, 12, 0.4, 0.4, 0.4, 0.03);

        // sounds: crackle every tick, thunder every 8 ticks, deep rumble every 20
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS,
                0.4F, 1.5F + RANDOM.nextFloat() * 0.5F);
        if (tickCounter % 8 == 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.ELECTRO_ZAP.get(), SoundSource.WEATHER,
                    1.0F, 1.2F + RANDOM.nextFloat() * 0.3F);
        }
        if (tickCounter % 20 == 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.ELECTRO_BLAST.get(), SoundSource.WEATHER,
                    2.0F, 0.8F + RANDOM.nextFloat() * 0.2F);
        }
    }

}
