package com.rumblefruit;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// server-side stance combat: electro sword slashes and electro bow shots
public class StanceCombat {
    private static final long SLASH_COOLDOWN = 18; // ticks
    private static final long BOW_COOLDOWN = 14;
    private static final Map<UUID, Long> LAST_SLASH = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_SHOT = new ConcurrentHashMap<>();

    // sword slash: melee cone in front, epic-fight style chained strikes
    public static void slash(ServerPlayer player, int combo) {
        if (StanceData.get(player.getUUID()) != StanceData.SWORD) {
            return;
        }
        long now = player.level().getGameTime();
        if (now - LAST_SLASH.getOrDefault(player.getUUID(), -100L) < SLASH_COOLDOWN) {
            return;
        }
        LAST_SLASH.put(player.getUUID(), now);

        ServerLevel level = (ServerLevel) player.level();
        boolean holy = WingsData.isActive(player.getUUID());
        Vec3 eye = player.getEyePosition();
        Vec3 view = player.getLookAngle();
        float damage = holy ? 20.0F : 13.0F;
        double reach = 3.6;

        boolean hitAny = false;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(reach + 1.0),
                e -> e != player && e.isAlive())) {
            Vec3 to = entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0).subtract(eye);
            double dist = to.length();
            if (dist > reach || dist < 0.01) {
                continue;
            }
            if (view.dot(to.normalize()) > 0.5) { // ~60° cone
                entity.hurt(level.damageSources().playerAttack(player), damage);
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 12, 1));
                entity.knockback(0.5, -view.x, -view.z);
                hitAny = true;
                level.sendParticles(ModParticles.ELECTRO_SPARK.get(),
                        entity.getX(), entity.getY() + entity.getBbHeight() * 0.6, entity.getZ(),
                        8, 0.3, 0.3, 0.3, 0.04);
            }
        }

        // blue 3d slash arc in front of the player
        Vec3 center = eye.add(view.scale(2.2)).add(0.0, -0.3, 0.0);
        level.sendParticles(ModParticles.ELECTRO_SLASH.get(),
                center.x, center.y, center.z, 0,
                (float) Math.toRadians(-player.getYRot()), (float) Math.toRadians(player.getXRot()), 0.0F, 0.0);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.2F);
        if (hitAny) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.ELECTRO_ZAP.get(), SoundSource.PLAYERS, 0.8F, 1.5F);
        }
        // stickman-fight feel: each combo hurls the fighter across the ground
        Vec3 perp = new Vec3(-view.z, 0.0, view.x); // strafe direction
        switch (combo % 3) {
            case 0 -> { // spin: wide arc — sweeps forward and around the target
                player.push(view.x * 0.6 + perp.x * 0.55, 0.08, view.z * 0.6 + perp.z * 0.55);
            }
            case 1 -> { // leap slash: springs forward and up
                player.push(view.x * 0.5, 0.52, view.z * 0.5);
            }
            case 2 -> { // thrust: long dash straight through
                player.push(view.x * 1.25, 0.06, view.z * 1.25);
            }
        }
        player.hurtMarked = true;
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        net.neoforged.neoforge.network.PacketDistributor.sendToAllPlayers(
                new CombatAnimPacket(player.getUUID(), combo));
    }

    // fist strike: melee hit with electricity-charged fists and feet.
    // move: 0 = jab, 1 = cross, 2 = uppercut (launches), 3 = roundhouse kick (heavy knockback)
    public static void punch(ServerPlayer player, int move) {
        if (StanceData.get(player.getUUID()) != StanceData.FISTS) {
            return;
        }
        long now = player.level().getGameTime();
        if (now - LAST_SLASH.getOrDefault(player.getUUID(), -100L) < 15) {
            return;
        }
        LAST_SLASH.put(player.getUUID(), now);

        ServerLevel level = (ServerLevel) player.level();
        boolean holy = WingsData.isActive(player.getUUID());
        Vec3 eye = player.getEyePosition();
        Vec3 view = player.getLookAngle();
        boolean kick = move % 4 == 3 || move % 4 == 0; // hook kick and roundhouse
        boolean uppercut = move % 4 == 2;
        float damage = (holy ? 14.0F : 9.0F) * (kick ? 1.5F : uppercut ? 1.25F : 1.0F);
        double reach = 3.0;

        boolean hitAny = false;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(reach + 1.0),
                e -> e != player && e.isAlive())) {
            Vec3 to = entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0).subtract(eye);
            double dist = to.length();
            if (dist > reach || dist < 0.01) {
                continue;
            }
            if (view.dot(to.normalize()) > 0.5) {
                entity.hurt(level.damageSources().playerAttack(player), damage);
                if (uppercut) {
                    entity.push(0.0, 0.7, 0.0); // launched
                } else {
                    entity.knockback(kick ? 1.4 : 0.6, -view.x, -view.z);
                }
                entity.hurtMarked = true;
                hitAny = true;
                level.sendParticles(ModParticles.ELECTRO_SPARK.get(),
                        entity.getX(), entity.getY() + entity.getBbHeight() * 0.6, entity.getZ(),
                        10, 0.3, 0.3, 0.3, 0.05);
            }
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                kick ? SoundEvents.PLAYER_ATTACK_KNOCKBACK : SoundEvents.PLAYER_ATTACK_STRONG,
                SoundSource.PLAYERS, 1.0F, 1.1F);
        if (hitAny) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.ELECTRO_ZAP.get(), SoundSource.PLAYERS, 0.7F, 1.7F);
        }
        // step into the strike; the kick hops forward
        player.push(view.x * (kick ? 0.5 : 0.3), kick ? 0.12 : 0.0, view.z * (kick ? 0.5 : 0.3));
        player.hurtMarked = true;
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        net.neoforged.neoforge.network.PacketDistributor.sendToAllPlayers(
                new CombatAnimPacket(player.getUUID(), 10 + move % 4));
    }

    // bow release: fire an electro arrow, speed scales with the draw time
    public static void bowRelease(ServerPlayer player, int chargeTicks) {
        if (StanceData.get(player.getUUID()) != StanceData.BOW) {
            return;
        }
        long now = player.level().getGameTime();
        if (now - LAST_SHOT.getOrDefault(player.getUUID(), -100L) < BOW_COOLDOWN) {
            return;
        }
        LAST_SHOT.put(player.getUUID(), now);

        ServerLevel level = (ServerLevel) player.level();
        float power = net.minecraft.world.item.BowItem.getPowerForTime(Math.min(chargeTicks, 30));
        if (power < 0.15F) {
            power = 0.15F;
        }
        ElectroArrowEntity arrow = new ElectroArrowEntity(level, player);
        arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, power * 3.2F, 0.6F);
        level.addFreshEntity(arrow);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.2F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.ELECTRO_ZAP.get(), SoundSource.PLAYERS, 0.5F, 1.8F);
    }
}
