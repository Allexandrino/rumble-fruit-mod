package com.rumblefruit;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// R: "Release" — the alastor-tier ultimate. the player rises into the air while
// lightning pours out of the body, then detonates the fruit's entire power in a
// single nuke-class blast. afterwards the fruit is spent: all powers are gone
// until another lightning fruit is eaten.
public class ReleaseSkill {
    private static final int CHARGE_TICKS = 60; // 3s ascension
    private static final Map<UUID, Integer> ACTIVE = new ConcurrentHashMap<>();
    private static final Random RANDOM = new Random();

    public static boolean isActive(UUID playerId) {
        return ACTIVE.containsKey(playerId);
    }

    public static void begin(ServerPlayer player) {
        ACTIVE.put(player.getUUID(), 0);
        // broadcast the ascension pose (arms thrown skyward) to every client
        net.neoforged.neoforge.network.PacketDistributor.sendToAllPlayers(
                new CombatAnimPacket(player.getUUID(), 9));
        // survive your own ascension
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, CHARGE_TICKS + 40, 4, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.GLOWING, CHARGE_TICKS + 20, 0, false, false));
        ServerLevel level = (ServerLevel) player.level();
        level.playSound(null, player.blockPosition(),
                SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 3.0F, 0.6F);
        level.playSound(null, player.blockPosition(),
                SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 2.0F, 0.5F);
    }

    public static void tick(ServerPlayer player) {
        Integer t0 = ACTIVE.get(player.getUUID());
        if (t0 == null) {
            return;
        }
        int t = t0 + 1;
        ServerLevel level = (ServerLevel) player.level();
        if (t <= CHARGE_TICKS) {
            ascension(player, level, t);
            ACTIVE.put(player.getUUID(), t);
        } else {
            ACTIVE.remove(player.getUUID());
            detonate(player, level);
        }
    }

    // rising ~20 blocks into the sky while electricity tears out of the body
    private static void ascension(ServerPlayer player, ServerLevel level, int t) {
        player.fallDistance = 0.0F;
        Vec3 vel = player.getDeltaMovement();
        // accelerating climb: ~20 blocks over the charge, then hover
        double up = t < 8 ? 0.12 : t < 52 ? 0.48 : 0.0;
        player.setDeltaMovement(vel.x * 0.4, up, vel.z * 0.4);
        player.hurtMarked = true;

        // THE POWER COMES FROM WITHIN: jagged lightning rays burst out of the
        // caster's HANDS and FEET — more limbs join in as the charge builds
        Vec3 core = player.position().add(0.0, 1.2, 0.0);
        float yawRad = player.getYRot() * 0.0174533F;
        double rx = Math.cos(yawRad), rz = -Math.sin(yawRad);
        Vec3[] limbs = {
                player.position().add(rx * 0.45, 1.35, rz * 0.45),   // right hand
                player.position().add(-rx * 0.45, 1.35, -rz * 0.45), // left hand
                player.position().add(rx * 0.15, 0.15, rz * 0.15),   // right foot
                player.position().add(-rx * 0.15, 0.15, -rz * 0.15), // left foot
        };
        int activeLimbs = t < 15 ? 1 : t < 30 ? 2 : t < 45 ? 3 : 4;
        for (int i = 0; i < activeLimbs; i++) {
            Vec3 limb = limbs[(i + t) % 4];
            for (int j = 0; j < 2; j++) {
                double yaw = RANDOM.nextDouble() * Math.PI * 2.0;
                double pitch = (RANDOM.nextDouble() - 0.4) * 1.8;
                ray(level, limb, yaw, pitch, 8.0 + RANDOM.nextDouble() * 8.0);
            }
        }
        // the caster BECOMES electricity: a shell of sparks collapses onto the
        // body and the coating thickens with every tick
        double shell = Math.max(0.35, 2.6 - t * 0.038);
        int coating = 5 + t / 2;
        for (int i = 0; i < coating; i++) {
            double theta = RANDOM.nextDouble() * Math.PI * 2.0;
            double phi = RANDOM.nextDouble() * Math.PI;
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    core.x + Math.sin(phi) * Math.cos(theta) * shell,
                    core.y + Math.cos(phi) * shell,
                    core.z + Math.sin(phi) * Math.sin(theta) * shell,
                    1, 0.02, 0.02, 0.02, 0.0);
        }
        // sky bolts crash down INTO the caster, feeding the charge
        if (t % 4 == 0) {
            ElectroBolts.visual(level, player.getX(), player.getY() - 1.0, player.getZ(), player);
        }
        // the sky itself tears open: giant glowing gashes overhead
        if (t % 10 == 0) {
            double angle = RANDOM.nextDouble() * Math.PI * 2.0;
            level.sendParticles(ModParticles.ELECTRO_SLASH.get(),
                    player.getX() + Math.cos(angle) * 12.0, player.getY() + 22.0,
                    player.getZ() + Math.sin(angle) * 12.0,
                    0, (float) angle, (float) (RANDOM.nextDouble() - 0.5), 0.0F, 0.0);
        }
        // storm clouds boil together above the ascension
        for (int i = 0; i < 5; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2.0;
            double rr = RANDOM.nextDouble() * (4.0 + t * 0.2);
            level.sendParticles(ParticleTypes.CLOUD,
                    player.getX() + Math.cos(angle) * rr,
                    player.getY() + 16.0 + RANDOM.nextDouble() * 3.0,
                    player.getZ() + Math.sin(angle) * rr,
                    1, 0.0, -0.03, 0.0, 0.0);
        }
        // rotating double helix of energy climbing around the body
        for (int i = 0; i < 4; i++) {
            double angle = t * 0.35 + i * (Math.PI / 3.0);
            double rr = 2.2;
            double py = player.getY() + ((t * 0.6 + i * 0.7) % 4.0) - 0.5;
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    player.getX() + Math.cos(angle) * rr, py, player.getZ() + Math.sin(angle) * rr,
                    2, 0.05, 0.05, 0.05, 0.02);
            level.sendParticles(ParticleTypes.END_ROD,
                    player.getX() - Math.cos(angle) * rr, py, player.getZ() - Math.sin(angle) * rr,
                    1, 0.05, 0.05, 0.05, 0.01);
        }
        // pillar of light from the ground up to the caster
        if (t % 3 == 0) {
            for (double dy = 0.0; dy < 22.0; dy += 1.5) {
                level.sendParticles(ParticleTypes.END_ROD,
                        player.getX(), player.getY() - dy, player.getZ(), 1, 0.15, 0.05, 0.15, 0.0);
            }
        }
        // raw power aura
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                player.getX(), player.getY() + 1.0, player.getZ(), 14, 2.0, 1.5, 2.0, 0.1);
        level.sendParticles(ParticleTypes.FLASH,
                player.getX(), player.getY() + 1.0, player.getZ(), t % 20 == 0 ? 1 : 0, 0, 0, 0, 0);
        if (t % 8 == 0) {
            level.playSound(null, player.blockPosition(), SoundEvents.BEACON_AMBIENT,
                    SoundSource.PLAYERS, 3.0F, 0.5F + t * 0.02F);
            level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT,
                    SoundSource.WEATHER, 2.0F, 0.8F + t * 0.012F);
        }
    }

    // the nuke: magic damage ("slain by magic"), massive knockback, blinding flash
    private static void detonate(ServerPlayer player, ServerLevel level) {
        Vec3 center = player.position();
        double radius = 40.0;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(radius), e -> e != player && e.isAlive())) {
            double dist = entity.position().distanceTo(center);
            if (dist > radius) {
                continue;
            }
            float damage = (float) (1000.0 * (1.0 - dist / (radius * 1.5)));
            entity.hurt(level.damageSources().indirectMagic(player, player), damage);
            Vec3 away = entity.position().subtract(center).normalize()
                    .scale((1.0 - dist / radius) * 9.0);
            entity.push(away.x, 2.0, away.z);
            entity.hurtMarked = true;
            entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
        }

        // the blast: triple flash + expanding particle shells + double ring of bolts
        for (int i = 0; i < 3; i++) {
            level.sendParticles(ParticleTypes.FLASH, center.x, center.y + 1.0, center.z, 1, 0, 0, 0, 0);
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y + 1.0, center.z,
                700, 16.0, 10.0, 16.0, 0.5);
        level.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 1.0, center.z,
                350, 12.0, 8.0, 12.0, 0.4);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y + 1.0, center.z,
                10, 5.0, 3.0, 5.0, 0.0);
        level.sendParticles(ParticleTypes.FLAME, center.x, center.y + 1.0, center.z,
                80, 10.0, 6.0, 10.0, 0.15);
        // shockwave rings on the ground
        for (double ring = 6.0; ring <= 24.0; ring += 6.0) {
            int count = (int) (ring * 4.0);
            for (int i = 0; i < count; i++) {
                double angle = i * Math.PI * 2.0 / count;
                level.sendParticles(ParticleTypes.END_ROD,
                        center.x + Math.cos(angle) * ring, center.y + 0.3, center.z + Math.sin(angle) * ring,
                        1, 0.0, 0.4, 0.0, 0.05);
            }
        }
        // double crown of lightning
        for (int i = 0; i < 24; i++) {
            double angle = i * Math.PI / 12.0;
            double rr = i % 2 == 0 ? 8.0 : 16.0;
            ElectroBolts.visual(level, center.x + Math.cos(angle) * rr, center.y - 18.0,
                    center.z + Math.sin(angle) * rr, player);
        }
        // the final burst: a fan of jagged rays erupts out of the caster
        Vec3 core = center.add(0.0, 1.2, 0.0);
        for (int i = 0; i < 20; i++) {
            double yaw = RANDOM.nextDouble() * Math.PI * 2.0;
            double pitch = (RANDOM.nextDouble() - 0.3) * Math.PI;
            ray(level, core, yaw, pitch, 14.0 + RANDOM.nextDouble() * 10.0);
        }
        // the sky shatters: a crown of giant gashes tears across the heavens
        for (int i = 0; i < 5; i++) {
            double angle = i * Math.PI / 5.0;
            level.sendParticles(ModParticles.ELECTRO_SLASH.get(),
                    center.x + Math.cos(angle) * 16.0, center.y + 14.0,
                    center.z + Math.sin(angle) * 16.0,
                    0, (float) angle, (float) (RANDOM.nextDouble() - 0.5), 0.0F, 0.0);
        }
        // giant slash arcs across the sky
        for (int i = 0; i < 4; i++) {
            double angle = i * Math.PI / 3.0;
            level.sendParticles(ModParticles.ELECTRO_SLASH.get(),
                    center.x + Math.cos(angle) * 10.0, center.y + 2.0, center.z + Math.sin(angle) * 10.0,
                    0, (float) angle, 0.0F, 0.0F, 0.0);
        }
        // the crater: find the ground below and blow it open
        Vec3 ground = center;
        var hit = level.clip(new net.minecraft.world.level.ClipContext(
                center, center.add(0.0, -40.0, 0.0),
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, player));
        if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            ground = hit.getLocation();
        }
        level.explode(null, ground.x, ground.y + 1.0, ground.z, 16.0F, Level.ExplosionInteraction.BLOCK);
        level.explode(null, ground.x, ground.y + 3.0, ground.z, 11.0F, Level.ExplosionInteraction.BLOCK);
        level.explode(null, center.x, center.y, center.z, 8.0F, Level.ExplosionInteraction.NONE);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 16.0F, 0.3F);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 8.0F, 0.4F);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.ENDER_DRAGON_DEATH, SoundSource.PLAYERS, 6.0F, 1.4F);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 8.0F, 0.4F);

        // the caster is blasted DOWN into the crater — sprawled, then lies there
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 4, false, false));
        player.setDeltaMovement(0.0, -1.9, 0.0);
        player.hurtMarked = true;
        net.neoforged.neoforge.network.PacketDistributor.sendToAllPlayers(
                new CombatAnimPacket(player.getUUID(), 20));

        // the fruit is spent: wings fold, stance drops, powers are gone
        if (WingsData.isActive(player.getUUID())) {
            WingsData.setActive(player, false);
        }
        StanceData.reset(player);
        RumblePowerData.revoke(player);
        player.displayClientMessage(Component.translatable("rumblefruit.release_spent")
                .withStyle(net.minecraft.ChatFormatting.GOLD), true);
    }

    // jagged 3d lightning ray from a point: a crooked polyline of spark particles,
    // forking once near the middle — reads as real lightning, not a laser
    private static void ray(ServerLevel level, Vec3 from, double yaw, double pitch, double length) {
        double dx = Math.cos(yaw) * Math.cos(pitch);
        double dy = Math.sin(pitch);
        double dz = Math.sin(yaw) * Math.cos(pitch);
        Vec3 pos = from;
        int segments = 14;
        double segLen = length / segments;
        for (int i = 0; i < segments; i++) {
            // wander the direction a little each segment — that is the zigzag
            dx += (RANDOM.nextDouble() - 0.5) * 0.55;
            dy += (RANDOM.nextDouble() - 0.5) * 0.55;
            dz += (RANDOM.nextDouble() - 0.5) * 0.55;
            double norm = Math.sqrt(dx * dx + dy * dy + dz * dz);
            pos = pos.add(dx / norm * segLen, dy / norm * segLen, dz / norm * segLen);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z,
                    2, 0.03, 0.03, 0.03, 0.0);
            level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z,
                    1, 0.0, 0.0, 0.0, 0.0);
            // one fork branching off mid-ray
            if (i == segments / 2 && RANDOM.nextBoolean()) {
                ray(level, pos, yaw + (RANDOM.nextDouble() - 0.5) * 1.5,
                        pitch + (RANDOM.nextDouble() - 0.5) * 0.8, length * 0.4);
            }
        }
    }
}
