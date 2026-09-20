package com.rumblefruit;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;

// bow stance skills ("Перо Бури"):
// Z = Charged Shot (pierces up to 3 enemies), X = Arrow Rain (10 arrows over the area),
// C = Thunder Arrow (lightning at the impact point), V = Wind Wings (repulsion gust)
public class BowSkills {

    // Z: a heavy piercing shot that goes through up to 3 enemies
    public static void chargedShot(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        ElectroArrowEntity arrow = new ElectroArrowEntity(level, player);
        arrow.setPiercing(3);
        arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 4.5F, 0.3F);
        level.addFreshEntity(arrow);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.ELECTRO_ZAP.get(), SoundSource.PLAYERS, 1.2F, 0.7F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.5F, 0.6F);
    }

    // X: arrow rain — 10 electro arrows fall over the target area
    public static void arrowRain(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 target = SkillExecutor.rayTracePublic(player, 35.0);
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 10; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double r = random.nextDouble() * 6.0;
            ElectroArrowEntity arrow = new ElectroArrowEntity(level, player);
            arrow.setPos(target.x + Math.cos(angle) * r, target.y + 14.0 + random.nextDouble() * 3.0,
                    target.z + Math.sin(angle) * r);
            arrow.setDeltaMovement(0.0, -1.8, 0.0);
            level.addFreshEntity(arrow);
        }
        level.playSound(null, target.x, target.y, target.z,
                ModSounds.ELECTRO_BLAST.get(), SoundSource.WEATHER, 3.0F, 1.3F);
        level.sendParticles(ModParticles.ELECTRO_SPARK.get(), target.x, target.y + 6, target.z, 40, 5.0, 2.0, 5.0, 0.08);
    }

    // C: thunder arrow — a golden lightning strike where the arrow lands
    public static void thunderArrow(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        ElectroArrowEntity arrow = new ElectroArrowEntity(level, player);
        arrow.setPillarOnImpact(true);
        arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 3.5F, 0.4F);
        level.addFreshEntity(arrow);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.ELECTRO_ZAP.get(), SoundSource.PLAYERS, 1.5F, 0.7F);
    }

    // V: wind wings — a gust that blasts every nearby enemy away
    public static void windWings(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        boolean holy = WingsData.isActive(player.getUUID());
        double radius = holy ? 14.0 : 10.0;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                e -> e != player && e.isAlive())) {
            Vec3 push = entity.position().subtract(player.position()).normalize()
                    .scale(holy ? 4.0 : 3.0);
            entity.push(push.x, 0.8, push.z);
            entity.hurtMarked = true;
            entity.hurt(level.damageSources().indirectMagic(player, player), holy ? 14.0F : 8.0F);
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3));
        }
        level.sendParticles(ModParticles.ELECTRO_CLOUD.get(), player.getX(), player.getY() + 1.2, player.getZ(),
                60, 3.0, 1.0, 3.0, 0.15);
        level.sendParticles(ModParticles.ELECTRO_SPARK.get(), player.getX(), player.getY() + 1.0, player.getZ(),
                40, 3.0, 1.0, 3.0, 0.1);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 2.0F, 1.2F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.ELECTRO_BLAST.get(), SoundSource.WEATHER, 2.0F, 1.5F);
    }
}
