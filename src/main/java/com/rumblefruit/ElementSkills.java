package com.rumblefruit;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// elemental skill sets: every fruit element gets its OWN skills with its own
// shapes, visuals and sounds — not a recolor of the lightning kit.
// Z/X/C/V per element, all built from vacuum-safe primitives
public final class ElementSkills {

    private ElementSkills() {
    }

    public static void cast(Element element, ServerPlayer player, int skillId, int variantOrCharge) {
        ServerLevel level = (ServerLevel) player.level();
        switch (skillId) {
            case 0 -> zSkill(element, player, level);
            case 1 -> xSkill(element, player, level);
            case 2 -> cSkill(element, player, level);
            case 5 -> vSkill(element, player, level);
            default -> {
            }
        }
    }

    // ---------------- Z: the signature strike ----------------
    private static void zSkill(Element element, ServerPlayer player, ServerLevel level) {
        switch (element) {
            case INFERNO -> { // Flame Jet: a cone of searing fire, ignites everything
                coneDamage(player, level, 12.0, 2.2, 14.0F, element);
                jetFx(level, player, element, 12.0, 60);
                sound(level, player, element.castSound(), 1.5F, 0.9F);
            }
            case VOID -> { // Shadow Rift: blink forward, shredding everything you pass
                Vec3 from = player.position();
                Vec3 look = player.getLookAngle();
                Vec3 to = from.add(look.x * 12.0, 0.0, look.z * 12.0);
                lineDamage(player, level, from, to, 2.0, 16.0F, element);
                player.teleportTo(to.x, to.y, to.z);
                for (double d = 0.0; d < 12.0; d += 0.7) {
                    Vec3 p = from.add(look.x * d, 1.0, look.z * d);
                    level.sendParticles(element.spark(), p.x, p.y, p.z, 3, 0.1, 0.3, 0.1, 0.02);
                }
                sound(level, player, element.castSound(), 1.5F, 0.7F);
            }
            case FROST -> { // Ice Lance: a piercing hitscan shard that deep-freezes
                pierceDamage(player, level, 16.0, 3, 18.0F, element);
                jetFx(level, player, element, 16.0, 50);
                sound(level, player, element.castSound(), 1.2F, 1.1F);
            }
            case NATURE -> { // Vine Lash: whip the closest target and drag it to you
                LivingEntity victim = nearest(player, level, 14.0);
                if (victim != null) {
                    Vec3 pull = player.position().subtract(victim.position()).normalize().scale(2.2);
                    victim.push(pull.x, 0.5, pull.z);
                    victim.hurtMarked = true;
                    hurt(level, player, victim, 13.0F, element);
                }
                jetFx(level, player, element, 14.0, 40);
                sound(level, player, element.castSound(), 1.2F, 1.0F);
            }
            default -> {
            }
        }
    }

    // ---------------- X: the area blast ----------------
    private static void xSkill(Element element, ServerPlayer player, ServerLevel level) {
        switch (element) {
            case INFERNO -> { // Flame Ring: a burning nova around the caster
                areaDamage(player, level, 9.0, 16.0F, element);
                shatterFx(level, player.position(), element, 9.0);
                explodeSafe(level, player, player.getX(), player.getY() + 1.0, player.getZ(), 3.0F,
                        false);
                sound(level, player, element.castSound(), 2.0F, 0.8F);
            }
            case VOID -> { // Gravity Well: drag everything in to one point and crush it
                Vec3 well = player.position().add(player.getLookAngle().x * 8.0, 1.0,
                        player.getLookAngle().z * 8.0);
                for (LivingEntity e : nearby(player, level, 14.0)) {
                    Vec3 pull = well.subtract(e.position()).normalize().scale(2.0);
                    e.push(pull.x, 0.3, pull.z);
                    e.hurtMarked = true;
                    hurt(level, player, e, 15.0F, element);
                }
                level.sendParticles(element.spark(), well.x, well.y, well.z, 120, 1.5, 1.5, 1.5, 0.1);
                sound(level, player, element.castSound(), 2.0F, 0.6F);
            }
            case FROST -> { // Blizzard: a lattice of razor ice shards, everything freezes stiff
                areaDamage(player, level, 10.0, 10.0F, element);
                // straight shard lines in a hard lattice — no circles
                for (int arm = 0; arm < 6; arm++) {
                    double a = arm * Math.PI / 3.0;
                    for (double d = 0.5; d <= 10.0; d += 0.8) {
                        level.sendParticles(element.spark(),
                                player.getX() + Math.cos(a) * d,
                                player.getY() + 0.3 + (d % 2.5) * 0.9,
                                player.getZ() + Math.sin(a) * d, 1, 0.0, 0.0, 0.0, 0.0);
                    }
                }
                sound(level, player, element.castSound(), 2.0F, 0.9F);
            }
            case NATURE -> { // Bloom Burst: friends mend, enemies rot
                player.heal(8.0F);
                areaDamage(player, level, 9.0, 12.0F, element);
                shatterFx(level, player.position(), element, 9.0);
                sound(level, player, element.castSound(), 1.5F, 1.2F);
            }
            default -> {
            }
        }
    }

    // ---------------- C: the targeted zone ----------------
    private static void cSkill(Element element, ServerPlayer player, ServerLevel level) {
        Vec3 target = rayTrace(player, 40.0);
        switch (element) {
            case INFERNO -> { // Flame Pillar: a column of roaring fire on the mark
                pillarDamage(player, level, target, 6.0, 30.0F, element);
                for (double dy = 0.0; dy < 20.0; dy += 1.2) {
                    level.sendParticles(element.spark(), target.x, target.y + dy, target.z,
                            8, 0.6, 0.2, 0.6, 0.05);
                }
                explodeSafe(level, player, target.x, target.y + 1.0, target.z, 3.0F,
                        false);
                sound(level, target, element.castSound(), 3.0F, 0.8F);
            }
            case VOID -> { // Void Drop: the marked ones are flung into the sky of the abyss
                for (LivingEntity e : at(level, player, target, 6.0)) {
                    e.push(0.0, 1.8, 0.0);
                    e.hurtMarked = true;
                    hurt(level, player, e, 25.0F, element);
                }
                level.sendParticles(element.spark(), target.x, target.y + 1.0, target.z,
                        150, 3.0, 4.0, 3.0, 0.05);
                sound(level, target, element.castSound(), 3.0F, 0.6F);
            }
            case FROST -> { // Ice Spikes: a line of spikes erupts from the ground
                Vec3 from = player.position();
                Vec3 step = target.subtract(from).normalize();
                for (double d = 2.0; d < 18.0; d += 2.0) {
                    Vec3 p = from.add(step.x * d, 0.0, step.z * d);
                    for (LivingEntity e : at(level, player, p, 2.2)) {
                        hurt(level, player, e, 20.0F, element);
                    }
                    level.sendParticles(element.spark(), p.x, p.y + 1.0, p.z, 15, 0.3, 1.2, 0.3, 0.02);
                }
                sound(level, target, element.castSound(), 2.5F, 1.0F);
            }
            case NATURE -> { // Root Prison: the marked are rooted where they stand
                for (LivingEntity e : at(level, player, target, 6.0)) {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 6));
                    hurt(level, player, e, 18.0F, element);
                }
                level.sendParticles(element.spark(), target.x, target.y + 0.5, target.z,
                        100, 3.0, 1.0, 3.0, 0.05);
                sound(level, target, element.castSound(), 2.0F, 1.1F);
            }
            default -> {
            }
        }
    }

    // ---------------- V: the ultimate ----------------
    private static void vSkill(Element element, ServerPlayer player, ServerLevel level) {
        Vec3 target = rayTrace(player, 60.0);
        switch (element) {
            case INFERNO -> { // Inferno Meteor: a burning moon falls on the mark
                FallingBlockEntity meteor = FallingBlockEntity.fall(level,
                        BlockPos.containing(target.x, target.y + 30.0, target.z),
                        Blocks.MAGMA_BLOCK.defaultBlockState());
                meteor.setDeltaMovement(0.0, -1.8, 0.0);
                areaDamage(player, level, target, 12.0, 45.0F, element);
                explodeSafe(level, player, target.x, target.y + 1.0, target.z, 6.0F,
                        true);
                // fire ring on the ground + a pillar of flame, not a sphere
                shatterFx(level, target, element, 10.0);
                for (double dy = 0.0; dy < 25.0; dy += 1.2) {
                    level.sendParticles(element.spark(), target.x, target.y + dy, target.z,
                            6, 0.5, 0.2, 0.5, 0.04);
                }
                FarFx.column(level, element.spark(), target.x, target.y, target.z, 80.0, 2.0, 3, 0.6);
                sound(level, target, element.castSound(), 4.0F, 0.6F);
            }
            case VOID -> { // Singularity: a flat vortex that inhales the battlefield
                for (LivingEntity e : at(level, player, target, 14.0)) {
                    Vec3 pull = target.subtract(e.position()).normalize().scale(2.5);
                    e.push(pull.x, 0.2, pull.z);
                    e.hurtMarked = true;
                    hurt(level, player, e, 40.0F, element);
                }
                // straight void beams converging from eight directions — hard
                // geometry, no circles
                for (int arm = 0; arm < 8; arm++) {
                    double a = arm * Math.PI / 4.0;
                    for (double d = 1.0; d <= 12.0; d += 0.8) {
                        level.sendParticles(element.spark(),
                                target.x + Math.cos(a) * d, target.y + 0.3 + (12.0 - d) * 0.12,
                                target.z + Math.sin(a) * d, 2, 0.0, 0.0, 0.0, 0.0);
                    }
                }
                level.sendParticles(element.spark(), target.x, target.y + 0.6, target.z,
                        30, 0.6, 0.3, 0.6, 0.02); // the dark core
                FarFx.column(level, element.spark(), target.x, target.y, target.z, 100.0, 2.0, 3, 0.7);
                sound(level, target, element.castSound(), 4.0F, 0.5F);
            }
            case FROST -> { // Glacial Spike: a mountain of ice falls from the sky
                areaDamage(player, level, target, 10.0, 42.0F, element);
                for (double dy = 0.0; dy < 30.0; dy += 1.5) {
                    level.sendParticles(element.spark(), target.x, target.y + dy, target.z,
                            12, 1.0 - dy * 0.02, 0.1, 1.0 - dy * 0.02, 0.03);
                }
                FarFx.column(level, element.spark(), target.x, target.y, target.z, 90.0, 2.0, 3, 0.6);
                sound(level, target, element.castSound(), 4.0F, 0.9F);
            }
            case NATURE -> { // World Root: the earth itself rises and feeds you
                areaDamage(player, level, target, 11.0, 38.0F, element);
                player.heal(player.getMaxHealth() * 0.5F);
                for (int i = 0; i < 16; i++) {
                    double a = i * Math.PI / 8.0;
                    for (double dy = 0.0; dy < 12.0; dy += 1.5) {
                        level.sendParticles(element.spark(),
                                target.x + Math.cos(a) * 3.0, target.y + dy, target.z + Math.sin(a) * 3.0,
                                3, 0.2, 0.1, 0.2, 0.02);
                    }
                }
                FarFx.column(level, element.spark(), target.x, target.y, target.z, 70.0, 2.0, 3, 0.6);
                sound(level, target, element.castSound(), 3.0F, 1.0F);
            }
            default -> {
            }
        }
    }

    // ---------------- shared mechanics ----------------
    private static Vec3 rayTrace(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 view = player.getLookAngle();
        Vec3 reach = eye.add(view.scale(range));
        var hit = player.level().clip(new net.minecraft.world.level.ClipContext(
                eye, reach, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, player));
        return hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS ? reach : hit.getLocation();
    }

    private static void hurt(ServerLevel level, ServerPlayer player, LivingEntity target,
                             float amount, Element element) {
        // transformed (F): skills hit 60% harder and carry empowered riders
        boolean empowered = WingsData.isActive(player.getUUID());
        target.hurt(level.damageSources().indirectMagic(player, player),
                empowered ? amount * 1.6F : amount);
        target.hurtMarked = true;
        element.applyRider(target, player, empowered);
    }

    private static java.util.List<LivingEntity> nearby(ServerPlayer player, ServerLevel level,
                                                       double radius) {
        return level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(radius), e -> e != player && e.isAlive());
    }

    private static java.util.List<LivingEntity> at(ServerLevel level, ServerPlayer player,
                                                   Vec3 center, double radius) {
        return level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center.x - radius, center.y - radius, center.z - radius,
                        center.x + radius, center.y + radius, center.z + radius),
                e -> e != player && e.isAlive());
    }

    private static LivingEntity nearest(ServerPlayer player, ServerLevel level, double range) {
        LivingEntity best = null;
        double bestDist = range;
        for (LivingEntity e : nearby(player, level, range)) {
            double d = e.position().distanceTo(player.position());
            if (d < bestDist) {
                bestDist = d;
                best = e;
            }
        }
        return best;
    }

    private static void coneDamage(ServerPlayer player, ServerLevel level, double range,
                                   double halfWidth, float damage, Element element) {
        Vec3 eye = player.getEyePosition();
        Vec3 view = player.getLookAngle();
        for (LivingEntity e : nearby(player, level, range + halfWidth)) {
            Vec3 to = e.position().add(0.0, e.getBbHeight() * 0.5, 0.0).subtract(eye);
            double along = to.dot(view);
            if (along < 0.0 || along > range) {
                continue;
            }
            if (eye.add(view.scale(along)).distanceTo(e.position().add(0.0, e.getBbHeight() * 0.5, 0.0)) <= halfWidth) {
                hurt(level, player, e, damage, element);
            }
        }
    }

    private static void pierceDamage(ServerPlayer player, ServerLevel level, double range,
                                     int maxTargets, float damage, Element element) {
        Vec3 eye = player.getEyePosition();
        Vec3 view = player.getLookAngle();
        int hit = 0;
        for (LivingEntity e : nearby(player, level, range + 2.0)) {
            if (hit >= maxTargets) {
                break;
            }
            Vec3 to = e.position().add(0.0, e.getBbHeight() * 0.5, 0.0).subtract(eye);
            double along = to.dot(view);
            if (along < 0.0 || along > range) {
                continue;
            }
            if (eye.add(view.scale(along)).distanceTo(e.position().add(0.0, e.getBbHeight() * 0.5, 0.0)) <= 1.5) {
                hurt(level, player, e, damage, element);
                hit++;
            }
        }
    }

    private static void lineDamage(ServerPlayer player, ServerLevel level, Vec3 from, Vec3 to,
                                   double halfWidth, float damage, Element element) {
        Vec3 span = to.subtract(from);
        double length = span.length();
        Vec3 dir = span.normalize();
        for (LivingEntity e : nearby(player, level, length + halfWidth + 2.0)) {
            Vec3 rel = e.position().subtract(from);
            double along = rel.dot(dir);
            if (along < 0.0 || along > length) {
                continue;
            }
            if (from.add(dir.scale(along)).distanceTo(e.position()) <= halfWidth) {
                hurt(level, player, e, damage, element);
            }
        }
    }

    private static void areaDamage(ServerPlayer player, ServerLevel level, double radius,
                                   float damage, Element element) {
        for (LivingEntity e : nearby(player, level, radius)) {
            hurt(level, player, e, damage, element);
        }
    }

    private static void pillarDamage(ServerPlayer player, ServerLevel level, Vec3 target,
                                     double radius, float damage, Element element) {
        for (LivingEntity e : at(level, player, target, radius)) {
            hurt(level, player, e, damage, element);
        }
    }

    private static void areaDamage(ServerPlayer player, ServerLevel level, Vec3 center,
                                   double radius, float damage, Element element) {
        for (LivingEntity e : at(level, player, center, radius)) {
            hurt(level, player, e, damage, element);
        }
    }

    // ---------------- visuals & sound ----------------
    // alastor-sharp: a needle-straight beam of light, no fuzz
    private static void jetFx(ServerLevel level, ServerPlayer player, Element element,
                              double range, int count) {
        Vec3 eye = player.getEyePosition();
        Vec3 view = player.getLookAngle();
        for (double d = 0.8; d < range; d += 0.45) {
            Vec3 p = eye.add(view.scale(d));
            level.sendParticles(element.spark(), p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    // our own blasts never hurt the caster: one instant of full resistance
    // covers the detonation frame
    private static void explodeSafe(ServerLevel level, ServerPlayer player, double x, double y, double z,
                                    float power, boolean breaksBlocks) {
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 4, 4, false, false));
        level.explode(null, x, y, z, power,
                breaksBlocks ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
    }

    // hard angular shatter: four straight diagonal beams crossing the point
    // (an X pattern) plus jagged slash arcs — no circles, ever
    private static void shatterFx(ServerLevel level, Vec3 center, Element element, double radius) {
        for (int arm = 0; arm < 4; arm++) {
            double a = arm * Math.PI / 2.0 + Math.PI / 4.0;
            for (double d = 1.0; d <= radius; d += 1.0) {
                level.sendParticles(element.spark(),
                        center.x + Math.cos(a) * d, center.y + 0.4 + (d % 3.0) * 0.3,
                        center.z + Math.sin(a) * d, 3, 0.0, 0.0, 0.0, 0.0);
            }
        }
        for (int i = 0; i < 3; i++) {
            level.sendParticles(ModParticles.ELECTRO_SLASH.get(),
                    center.x + (i - 1) * 3.0, center.y + 1.5, center.z + (i - 1) * 2.0,
                    0, (float) (i * 1.05), (float) (i * 0.5 - 0.5), 0.0F, 0.0);
        }
    }

    private static void sound(ServerLevel level, ServerPlayer player,
                              net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                sound, SoundSource.PLAYERS, volume, pitch);
    }

    private static void sound(ServerLevel level, Vec3 at,
                              net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.WEATHER, volume, pitch);
    }
}
