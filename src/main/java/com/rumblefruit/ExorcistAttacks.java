package com.rumblefruit;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

// the Fallen Exorcist.s arsenal: 25 distinct attacks. every pattern is built from
// vacuum-safe primitives (bolts, particles, sounds, explosions, effects) so
// the whole arsenal is unit-testable. the entity just calls perform().
public final class ExorcistAttacks {

    private ExorcistAttacks() {
    }

    public static final int COUNT = 25;
    static Random RANDOM = new Random();

    public static void perform(int index, ServerLevel level, LivingEntity boss, LivingEntity target) {
        Vec3 b = boss.position();
        Vec3 t = target.position();
        switch (Math.floorMod(index, COUNT)) {
            case 0 -> { // 1. Skull Crusher: a single crushing bolt on the head
                ElectroBolts.strike(level, t.x, t.y, t.z, null);
                hurt(level, boss, target, 15.0F);
            }
            case 1 -> { // 2. Ring of Judgment: 8 bolts encircle the prey
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4.0;
                    ElectroBolts.strike(level, t.x + Math.cos(a) * 4.0, t.y, t.z + Math.sin(a) * 4.0, null);
                }
            }
            case 2 -> { // 3. Cross of Storms: four bolts on the cardinal points
                for (int i = 0; i < 4; i++) {
                    double a = i * Math.PI / 2.0;
                    ElectroBolts.strike(level, t.x + Math.cos(a) * 3.0, t.y, t.z + Math.sin(a) * 3.0, null);
                }
            }
            case 3 -> { // 4. Heaven Rain: 12 random bolts pour around the prey
                for (int i = 0; i < 12; i++) {
                    ElectroBolts.strike(level, t.x + (RANDOM.nextDouble() - 0.5) * 14.0, t.y,
                            t.z + (RANDOM.nextDouble() - 0.5) * 14.0, null);
                }
            }
            case 4 -> { // 5. Spiral Out: bolts spiral away from the titan
                for (int i = 0; i < 14; i++) {
                    double a = i * 0.7;
                    double r = 2.0 + i * 0.8;
                    ElectroBolts.strike(level, b.x + Math.cos(a) * r, b.y, b.z + Math.sin(a) * r, null);
                }
            }
            case 5 -> { // 6. Homing Lance: reads the prey's movement and strikes ahead
                Vec3 v = target.getDeltaMovement();
                ElectroBolts.strike(level, t.x + v.x * 12.0, t.y, t.z + v.z * 12.0, null);
                ElectroBolts.strike(level, t.x, t.y, t.z, null);
            }
            case 6 -> { // 7. Double Tap
                ElectroBolts.strike(level, t.x, t.y, t.z, null);
                ElectroBolts.strike(level, t.x, t.y, t.z, null);
            }
            case 7 -> { // 8. Orb Fan: a fan of five bolts towards the prey
                Vec3 dir = t.subtract(b).normalize();
                for (int i = -2; i <= 2; i++) {
                    double a = Math.atan2(dir.z, dir.x) + i * 0.22;
                    ElectroBolts.strike(level, b.x + Math.cos(a) * 6.0, b.y, b.z + Math.sin(a) * 6.0, null);
                }
            }
            case 8 -> { // 9. Detonation Ring: 12 bolts blast everyone off the titan
                for (int i = 0; i < 12; i++) {
                    double a = i * Math.PI / 6.0;
                    ElectroBolts.strike(level, b.x + Math.cos(a) * 8.0, b.y, b.z + Math.sin(a) * 8.0, null);
                }
            }
            case 9 -> { // 10. Ground Slam: the floor itself detonates
                level.explode(null, b.x, b.y, b.z, 3.0F, Level.ExplosionInteraction.BLOCK);
                if (b.distanceTo(t) < 7.0) {
                    Vec3 away = t.subtract(b).normalize().scale(1.8);
                    target.push(away.x, 0.9, away.z);
                    hurt(level, boss, target, 20.0F);
                }
            }
            case 10 -> { // 11. Nova: raw discharge in every direction
                hurtNearby(level, boss, b, 8.0, 25.0F);
                burst(level, b, 150, 6.0);
            }
            case 11 -> { // 12. Blink Strike: the titan materialises on the prey
                boss.setPos(t.x + 1.5, t.y, t.z + 1.5);
                ElectroBolts.strike(level, t.x, t.y, t.z, null);
                hurt(level, boss, target, 22.0F);
            }
            case 12 -> { // 13. Spark Summons: six spirit bolts close in
                for (int i = 0; i < 6; i++) {
                    double a = RANDOM.nextDouble() * Math.PI * 2.0;
                    ElectroBolts.strike(level, t.x + Math.cos(a) * 5.0, t.y, t.z + Math.sin(a) * 5.0, null);
                }
            }
            case 13 -> { // 14. Sky Pillar: a column of light marks the kill zone
                FarFx.column(level, ModParticles.ELECTRO_GLOW.get(), t.x, t.y, t.z, 40.0, 2.0, 2, 0.3);
                ElectroBolts.strike(level, t.x, t.y, t.z, null);
            }
            case 14 -> { // 15. Mini Meteor: a burning cube falls on the prey
                FallingBlockEntity mini = FallingBlockEntity.fall(level,
                        net.minecraft.core.BlockPos.containing(t.x, t.y + 15.0, t.z),
                        Blocks.MAGMA_BLOCK.defaultBlockState());
                mini.setDeltaMovement(0.0, -1.8, 0.0);
                burst(level, new Vec3(t.x, t.y + 15.0, t.z), 20, 1.0);
            }
            case 15 -> { // 16. Lightning Cage: bolts pin the prey in place
                for (int i = 0; i < 12; i++) {
                    double a = i * Math.PI / 6.0;
                    ElectroBolts.visual(level, t.x + Math.cos(a) * 3.0, t.y, t.z + Math.sin(a) * 3.0, null);
                }
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3));
                hurt(level, boss, target, 12.0F);
            }
            case 16 -> { // 17. Twin Nova: detonations at the titan AND the prey
                level.explode(null, b.x, b.y, b.z, 4.0F, Level.ExplosionInteraction.NONE);
                level.explode(null, t.x, t.y, t.z, 4.0F, Level.ExplosionInteraction.NONE);
                hurtNearby(level, boss, b, 6.0, 18.0F);
                hurtNearby(level, boss, t, 6.0, 18.0F);
            }
            case 17 -> { // 18. Chain Lightning: the bolt jumps around the prey
                ElectroBolts.strike(level, t.x, t.y, t.z, null);
                for (int i = 0; i < 3; i++) {
                    ElectroBolts.strike(level, t.x + (RANDOM.nextDouble() - 0.5) * 6.0, t.y,
                            t.z + (RANDOM.nextDouble() - 0.5) * 6.0, null);
                }
            }
            case 18 -> { // 19. Triple Blink: three teleport strikes in a triangle
                for (int i = 0; i < 3; i++) {
                    double a = i * Math.PI * 2.0 / 3.0;
                    boss.setPos(t.x + Math.cos(a) * 2.0, t.y, t.z + Math.sin(a) * 2.0);
                    ElectroBolts.strike(level, t.x, t.y, t.z, null);
                }
                hurt(level, boss, target, 18.0F);
            }
            case 19 -> { // 20. Gravity Slam: the sky itself hurls the prey up
                target.push(0.0, 1.4, 0.0);
                target.hurtMarked = true;
                ElectroBolts.strike(level, t.x, t.y, t.z, null);
            }
            case 20 -> { // 21. Ray Fan: a fan of jagged rays out of the titan
                for (int i = 0; i < 12; i++) {
                    double a = RANDOM.nextDouble() * Math.PI * 2.0;
                    ray(level, b.add(0.0, 3.0, 0.0), a, (RANDOM.nextDouble() - 0.5) * 0.9,
                            10.0 + RANDOM.nextDouble() * 6.0);
                }
                hurtNearby(level, boss, t, 8.0, 16.0F);
            }
            case 21 -> { // 22. Storm Call: the whole arena boils with lightning
                for (int i = 0; i < 20; i++) {
                    double a = RANDOM.nextDouble() * Math.PI * 2.0;
                    double r = RANDOM.nextDouble() * 12.0;
                    ElectroBolts.strike(level, b.x + Math.cos(a) * r, b.y, b.z + Math.sin(a) * r, null);
                }
                level.playSound(null, b.x, b.y, b.z, SoundEvents.ENDER_DRAGON_GROWL,
                        SoundSource.HOSTILE, 3.0F, 0.6F);
            }
            case 22 -> { // 23. Iron Shield: the cube skin hardens
                boss.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 1));
                boss.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
                burst(level, b.add(0.0, 3.0, 0.0), 60, 4.0);
            }
            case 23 -> { // 24. Enrage: the titan burns white-hot
                boss.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
                burst(level, b.add(0.0, 3.0, 0.0), 120, 5.0);
                level.playSound(null, b.x, b.y, b.z, SoundEvents.ENDER_DRAGON_GROWL,
                        SoundSource.HOSTILE, 3.0F, 0.5F);
            }
            default -> { // 25. Executioner: a killing blast right on the prey
                level.explode(null, t.x, t.y, t.z, 5.0F, Level.ExplosionInteraction.NONE);
                ElectroBolts.strike(level, t.x, t.y, t.z, null);
                ElectroBolts.strike(level, t.x, t.y + 3.0, t.z, null);
                hurt(level, boss, target, 40.0F);
                burst(level, t, 200, 4.0);
            }
        }
    }

    private static void hurt(ServerLevel level, LivingEntity boss, LivingEntity target, float amount) {
        target.hurt(level.damageSources().indirectMagic(boss, boss), amount);
        target.hurtMarked = true;
    }

    private static void hurtNearby(ServerLevel level, LivingEntity boss, Vec3 at, double radius, float amount) {
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(at.x - radius, at.y - radius, at.z - radius,
                        at.x + radius, at.y + radius, at.z + radius),
                e -> e != boss && e.isAlive())) {
            entity.hurt(level.damageSources().indirectMagic(boss, boss), amount);
            entity.hurtMarked = true;
        }
    }

    private static void burst(ServerLevel level, Vec3 at, int count, double spread) {
        level.sendParticles(ModParticles.ELECTRO_SPARK.get(), at.x, at.y, at.z, count,
                spread, spread * 0.7, spread, 0.15);
    }

    private static void ray(ServerLevel level, Vec3 from, double yaw, double pitch, double length) {
        java.util.List<com.rumblefruit.core.Vec> points = com.rumblefruit.core.RayPolyline.generate(
                new com.rumblefruit.core.Vec(from.x, from.y, from.z), yaw, pitch, length, RANDOM);
        for (com.rumblefruit.core.Vec p : points) {
            level.sendParticles(ModParticles.ELECTRO_SPARK.get(), p.x(), p.y(), p.z(),
                    2, 0.03, 0.03, 0.03, 0.0);
        }
    }
}
