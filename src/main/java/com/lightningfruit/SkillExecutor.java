package com.lightningfruit;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

// Blox Fruits Lightning moveset:
// Z: tap=projectile, hold=pull+stun, long hold=dragon | X: storm clouds | C: lightning pillar
// F: toggle lightning wings (flight + exorcist mask) | V: thunderball (charged explosion)
@net.neoforged.fml.common.EventBusSubscriber(modid = LightningFruitMod.MOD_ID)
public class SkillExecutor {
    private static final double ORB_SPEED = 1.5;
    private static final Random RANDOM = new Random();

    // cooldowns in ticks: Z 3s, X 8s, C 12s, F 5s, V 5s
    private static final long[] COOLDOWNS = {60, 160, 240, 100, 100};
    private static final java.util.Map<java.util.UUID, long[]> LAST_USE = new java.util.HashMap<>();

    public static void execute(ServerPlayer player, int skillId, int variantOrCharge) {
        // abilities require: eaten the lightning fruit; weapon (sword/bow) OR bare hand for casting
        if (!LightningPowerData.hasPower(player)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "lightningfruit.need_eat").withStyle(net.minecraft.ChatFormatting.GRAY), true);
            return;
        }
        if (ReleaseSkill.isActive(player.getUUID())) {
            return; // mid-ascension: the ultimate is already running
        }
        if (skillId == 4) {
            return; // V press: charging is client-side, nothing to do on the server
        }
        if (skillId == 9) {
            // R: Release — rise, shed lightning, detonate everything (fruit is spent)
            System.out.println("[lightningfruit] Release started for " + player.getName().getString());
            player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
            ReleaseSkill.begin(player);
            return;
        }
        if (skillId == 8) {
            // F: toggle the angel transformation (needs charge in the power bar to activate)
            if (!WingsData.isActive(player.getUUID()) && !PowerChargeData.canTransform(player)) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        "lightningfruit.no_charge").withStyle(net.minecraft.ChatFormatting.GOLD), true);
                return;
            }
            WingsData.toggle(player);
            return;
        }
        int idx = skillId >= 0 && skillId <= 3 ? skillId : skillId == 5 ? 4 : -1;
        if (idx < 0) {
            return;
        }
        long now = player.level().getGameTime();
        long[] last = LAST_USE.computeIfAbsent(player.getUUID(), key -> new long[5]);
        // Z and F have NO cooldown — they are gated only by orbs (blox fruits style)
        boolean hasCooldown = skillId == 1 || skillId == 2 || skillId == 5;
        if (hasCooldown && now - last[idx] < COOLDOWNS[idx]) {
            long left = (COOLDOWNS[idx] - (now - last[idx]) + 19) / 20;
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "lightningfruit.cooldown", left).withStyle(net.minecraft.ChatFormatting.GRAY), true);
            return;
        }
        if (hasCooldown) {
            last[idx] = now;
        }
        // orb consumption (blox fruits): Z costs 1 orb; V costs charge-level orbs
        int orbCost = skillId == 0 ? 1 : skillId == 5 ? Math.max(1, variantOrCharge) : 0;
        int available = OrbManager.getOrbs(player.getUUID());
        if (orbCost > 0 && available <= 0) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "lightningfruit.no_orbs").withStyle(net.minecraft.ChatFormatting.GRAY), true);
            return;
        }
        if (skillId == 5) {
            variantOrCharge = Math.max(1, Math.min(variantOrCharge, available));
        }
        if (orbCost > 0) {
            OrbManager.consume(player, skillId == 5 ? variantOrCharge : orbCost);
        }
        // cast animation: vanilla synced arm swing (visible to everyone) + brief use-pose
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        player.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
        PENDING_POSE_STOP.put(player.getUUID(), now + 25);
        // stance-based skill sets: fists = blox fruits lightning,
        // sword = "Крыло Ангела", bow = "Перо Бури"
        int stance = StanceData.get(player.getUUID());
        switch (skillId) {
            case 0 -> { // Z
                if (stance == StanceData.SWORD) {
                    SwordSkills.severingStrike(player);
                } else if (stance == StanceData.BOW) {
                    BowSkills.chargedShot(player);
                } else {
                    lightningOrb(player, variantOrCharge);
                }
            }
            case 1 -> { // X
                if (stance == StanceData.SWORD) {
                    SwordSkills.heavenlySlash(player);
                } else if (stance == StanceData.BOW) {
                    BowSkills.arrowRain(player);
                } else {
                    thunderstorm(player);
                }
            }
            case 2 -> { // C
                if (stance == StanceData.SWORD) {
                    SwordSkills.execution(player);
                } else if (stance == StanceData.BOW) {
                    BowSkills.thunderArrow(player);
                } else {
                    lightningPillar(player);
                }
            }
            case 5 -> { // V
                if (stance == StanceData.SWORD) {
                    SwordSkills.angelStance(player);
                } else if (stance == StanceData.BOW) {
                    BowSkills.windWings(player);
                } else {
                    castThunderball(player, variantOrCharge);
                }
            }
        }
    }

    // stop the cast pose after 12 ticks
    private static final java.util.Map<java.util.UUID, Long> PENDING_POSE_STOP = new java.util.HashMap<>();

    @net.neoforged.bus.api.SubscribeEvent
    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Long stopAt = PENDING_POSE_STOP.get(player.getUUID());
        if (stopAt != null && player.level().getGameTime() >= stopAt) {
            if (player.isUsingItem()) {
                player.stopUsingItem();
            }
            PENDING_POSE_STOP.remove(player.getUUID());
        }
        ReleaseSkill.tick(player);
    }

    // angel transformation (wings out): skills hit harder and burn golden-white
    private static boolean holy(ServerPlayer player) {
        return WingsData.isActive(player.getUUID());
    }

    private static net.minecraft.core.particles.SimpleParticleType sparkType(boolean holy) {
        return holy ? net.minecraft.core.particles.ParticleTypes.END_ROD
                : net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK;
    }

    // Z variants: 0=tap projectile, 1=hold pull+stun, 2=long-hold dragon
    public static void lightningOrb(ServerPlayer player, int variant) {
        ServerLevel level = (ServerLevel) player.level();
        boolean holy = holy(player);
        switch (variant) {
            case 0 -> { // Z1: quick projectile — bow fires electricity, sword throws an orb
                if (holy) { // angel Z: Holy Spear — instant piercing golden lance
                    holySpear(player);
                    break;
                }
                if (StanceData.get(player.getUUID()) == StanceData.BOW) {
                    ElectroArrowEntity arrow = new ElectroArrowEntity(level, player);
                    arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 3.5F, 0.5F);
                    level.addFreshEntity(arrow);
                } else {
                    ElectroOrbEntity orb = new ElectroOrbEntity(level, player);
                    orb.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, (float) ORB_SPEED, 0.0F);
                    level.addFreshEntity(orb);
                }
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.8F, 1.6F);
            }
            case 1 -> { // Z2: pull enemies in + stun
                double radius = holy ? 13.0 : 10.0;
                for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().inflate(radius),
                        e -> e != player && e.isAlive())) {
                    Vec3 pull = player.position().subtract(entity.position()).normalize().scale(2.5);
                    entity.push(pull.x, 0.4, pull.z);
                    entity.hurtMarked = true;
                    entity.hurt(level.damageSources().indirectMagic(player, player), holy ? 26.0F : 15.0F);
                    entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 4)); // stun
                }
                level.sendParticles(sparkType(holy),
                        player.getX(), player.getY() + 1, player.getZ(), 50, 5.0, 1.5, 5.0, 0.08);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 3.0F, 0.8F);
            }
            case 2 -> { // Z3: electrified dragon — bigger, faster orb with more damage
                ElectroOrbEntity orb = new ElectroOrbEntity(level, player);
                orb.setDragonMode(true);
                orb.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, (float) (ORB_SPEED * 1.5), 0.0F);
                level.addFreshEntity(orb);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 2.0F, 1.2F);
            }
        }
    }

    public static void thunderstorm(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        boolean hasBow = StanceData.get(player.getUUID()) == StanceData.BOW;
        if (hasBow) {
            // bow X: volley of 5 electric arrows in a spread
            for (int i = -2; i <= 2; i++) {
                ElectroArrowEntity arrow = new ElectroArrowEntity(level, player);
                arrow.shootFromRotation(player, player.getXRot(), player.getYRot() + i * 4.0F, 0.0F, 3.0F, 0.5F);
                level.addFreshEntity(arrow);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.5F, 1.0F);
            return;
        }
        Vec3 target = rayTrace(player, 40.0);
        if (holy(player)) { // angel X: Judgment — golden pillars rain around the target
            judgment(player, target);
            return;
        }
        level.addFreshEntity(StormEntity.storm(level, target, player));
        // blox-fruits storm cloud: big dark cap that lingers over the area
        for (int i = 0; i < 60; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2.0;
            double r = RANDOM.nextDouble() * 10.0;
            level.sendParticles(ParticleTypes.CLOUD,
                    target.x + Math.cos(angle) * r,
                    target.y + 12.0 + RANDOM.nextDouble() * 2.0,
                    target.z + Math.sin(angle) * r,
                    1, 0.02, -0.01, 0.02, 0.0);
            level.sendParticles(ParticleTypes.SMOKE,
                    target.x + Math.cos(angle) * r * 0.9,
                    target.y + 11.0,
                    target.z + Math.sin(angle) * r * 0.9,
                    1, 0.0, -0.02, 0.0, 0.0);
        }
        level.playSound(null, target.x, target.y, target.z,
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 4.0F, 1.0F);
    }

    // C: lightning pillar — continuous beam from a thundercloud down to the target
    public static void lightningPillar(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        boolean hasBow = StanceData.get(player.getUUID()) == StanceData.BOW;
        if (hasBow) {
            // bow C: charged electric shot that calls the pillar on impact
            ElectroArrowEntity arrow = new ElectroArrowEntity(level, player);
            arrow.setPillarOnImpact(true);
            arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 3.0F, 0.5F);
            level.addFreshEntity(arrow);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.5F, 0.7F);
            return;
        }
        Vec3 target = rayTrace(player, 40.0);
        // sword C: stick the sword into the ground, then the pillar strikes around it
        if (StanceData.get(player.getUUID()) == StanceData.SWORD) {
            plantSword(level, target, player);
        }
        if (holy(player)) { // angel C: triple golden pillar in a line
            Vec3 view = player.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
            for (int i = -1; i <= 1; i++) {
                lightningPillarAt(level, target.add(view.scale(i * 5.0)), player);
            }
            return;
        }
        lightningPillarAt(level, target, player);
    }

    // angel Z: Holy Spear — instant piercing golden lance, damages everything in its path
    private static void holySpear(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 view = player.getLookAngle();
        Vec3 target = rayTrace(player, 25.0);
        double length = eye.distanceTo(target);
        // damage everything in a corridor along the beam
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().expandTowards(view.scale(length + 2.0)).inflate(2.0),
                e -> e != player && e.isAlive())) {
            Vec3 to = entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0).subtract(eye);
            double along = to.dot(view);
            if (along < 0.0 || along > length) {
                continue;
            }
            if (eye.add(view.scale(along)).distanceTo(entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0)) <= 2.0) {
                entity.hurt(level.damageSources().indirectMagic(player, player), 32.0F);
                entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0));
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
            }
        }
        // golden lance visuals: searing rod line + bolts striking along the path
        for (double d = 1.0; d < length; d += 0.5) {
            Vec3 p = eye.add(view.scale(d));
            level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
        }
        for (double d = 4.0; d < length; d += 6.0) {
            Vec3 p = eye.add(view.scale(d));
            ElectroBolts.visualHoly(level, p.x, p.y, p.z);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0F, 1.6F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 3.0F, 1.4F);
    }

    // angel X: Judgment — golden pillars rain around the target area
    private static void judgment(ServerPlayer player, Vec3 target) {
        ServerLevel level = (ServerLevel) player.level();
        for (int i = 0; i < 5; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2.0;
            double r = i == 0 ? 0.0 : 2.5 + RANDOM.nextDouble() * 4.5;
            Vec3 spot = target.add(Math.cos(angle) * r, 0.0, Math.sin(angle) * r);
            lightningPillarAt(level, spot, player);
        }
        level.playSound(null, target.x, target.y, target.z,
                SoundEvents.BEACON_DEACTIVATE, SoundSource.WEATHER, 2.0F, 1.2F);
    }

    // sword stuck into the ground at the target (invisible armor stand holding it upright)
    private static void plantSword(ServerLevel level, Vec3 target, ServerPlayer player) {
        var stand = new net.minecraft.world.entity.decoration.ArmorStand(level, target.x, target.y, target.z);
        stand.setInvisible(true);
        stand.setNoGravity(true);
        stand.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.item.ItemStack(LightningFruitMod.ELECTRO_SWORD.get()));
        level.addFreshEntity(stand);
        // crackling discharge around the planted sword
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.x, target.y + 1.0, target.z, 25, 0.4, 0.6, 0.4, 0.05);
    }

    // shared pillar effect at a position (used by sword C and charged bow shot)
    public static void lightningPillarAt(ServerLevel level, Vec3 target, net.minecraft.world.entity.player.Player player) {
        boolean holy = WingsData.isActive(player.getUUID());
        LightningPillarEntity.summon(level, target.x, target.y, target.z, holy);
        level.explode(null, target.x, target.y, target.z, 4.0F, Level.ExplosionInteraction.NONE);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(target.x - 8, target.y - 8, target.z - 8, target.x + 8, target.y + 8, target.z + 8),
                e -> e != player && e.isAlive())) {
            entity.hurt(level.damageSources().indirectMagic(player, player), holy ? 60.0F : 40.0F);
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3));
            entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
        }
        level.sendParticles(sparkType(holy),
                target.x, target.y + 1, target.z, 80, 6.0, 1.0, 6.0, 0.1);
    }

    public static int chargeLevel(int heldTicks) {
        if (heldTicks >= 30) return 4;
        if (heldTicks >= 15) return 3;
        if (heldTicks >= 5) return 2;
        return 1;
    }

    public static void castThunderball(ServerPlayer player, int charge) {
        ServerLevel level = (ServerLevel) player.level();
        if (holy(player)) {
            charge += 2; // angel V: the wrath ball grows far bigger
        }
        Vec3 target = rayTrace(player, 60.0);
        ThunderballEntity thunderball = new ThunderballEntity(ModEntities.THUNDERBALL.get(), level);
        thunderball.setChargeLevel(charge);
        thunderball.setOwner(player);
        thunderball.setTarget(target);
        thunderball.setPhase(ThunderballEntity.PHASE_FORMING);
        thunderball.setPos(player.getX(), player.getEyeY() + 2.5, player.getZ());
        level.addFreshEntity(thunderball);
        level.playSound(null, player.getX(), player.getEyeY() + 2.5, player.getZ(),
                SoundEvents.PORTAL_AMBIENT, SoundSource.WEATHER, 1.5F, 1.5F);
    }

    public static Vec3 rayTracePublic(ServerPlayer player, double range) {
        return rayTrace(player, range);
    }

    private static Vec3 rayTrace(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 view = player.getViewVector(1.0F);
        Vec3 reach = eye.add(view.scale(range));
        BlockHitResult hit = player.level().clip(new ClipContext(
                eye, reach, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? reach : hit.getLocation();
    }
}
