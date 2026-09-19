package com.lightningfruit;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Random;
import java.util.UUID;

public class ThunderballEntity extends Entity {
    private static final EntityDataAccessor<Integer> CHARGE_LEVEL =
            SynchedEntityData.defineId(ThunderballEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PHASE =
            SynchedEntityData.defineId(ThunderballEntity.class, EntityDataSerializers.INT);

    public static final int PHASE_FORMING = 0;
    public static final int PHASE_ASCENDING = 1;
    public static final int PHASE_FALLING = 2;
    public static final int PHASE_GROUNDED = 3;

    public static final int FORMING_TICKS = 25;
    private static final int ASCEND_MAX_TICKS = 45;
    private static final double ASCEND_SPEED = 1.2;
    private static final double HOVER_HEIGHT = 27.0;
    private static final int GROUND_PULSE_INTERVAL = 10;
    private static final int MAX_LIFE_TICKS = 400;

    private final Random random = new Random();
    private UUID ownerUUID;
    private Vec3 target = Vec3.ZERO;
    private int phaseStartTick = 0;

    public ThunderballEntity(EntityType<? extends ThunderballEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        builder.define(CHARGE_LEVEL, 1);
        builder.define(PHASE, PHASE_FORMING);
    }

    public int getChargeLevel() {
        return this.entityData.get(CHARGE_LEVEL);
    }

    public void setChargeLevel(int chargeLevel) {
        this.entityData.set(CHARGE_LEVEL, Math.max(1, Math.min(4, chargeLevel)));
    }

    public int getPhase() {
        return this.entityData.get(PHASE);
    }

    public void setPhase(int phase) {
        this.entityData.set(PHASE, phase);
        this.phaseStartTick = this.tickCount;
    }

    public int getPhaseStartTick() {
        return this.phaseStartTick;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (PHASE.equals(key) && this.level().isClientSide) {
            this.phaseStartTick = this.tickCount;
        }
        super.onSyncedDataUpdated(key);
    }

    public void setOwner(Player owner) {
        this.ownerUUID = owner.getUUID();
    }

    private Player getOwner() {
        return this.ownerUUID == null ? null : this.level().getPlayerByUUID(this.ownerUUID);
    }

    public void setTarget(Vec3 target) {
        this.target = target;
    }

    private int groundedTicks() {
        return 60 + 20 * getChargeLevel();
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            spawnParticles();
            return;
        }

        switch (getPhase()) {
            case PHASE_FORMING -> tickForming();
            case PHASE_ASCENDING -> tickAscending();
            case PHASE_FALLING -> tickFalling();
            case PHASE_GROUNDED -> tickGrounded();
        }

        if (this.tickCount > MAX_LIFE_TICKS) {
            finalBurst();
        }
    }

    private void tickForming() {
        Player owner = getOwner();
        if (owner != null && owner.isAlive() && !owner.isRemoved()) {
            this.setPos(owner.getX(), owner.getEyeY() + 2.5, owner.getZ());
        }
        this.setDeltaMovement(Vec3.ZERO);
        if (this.tickCount >= FORMING_TICKS) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER,
                    2.0F, 0.9F + random.nextFloat() * 0.2F);
            int flashes = 3 + random.nextInt(3);
            for (int i = 0; i < flashes; i++) {
                spawnLightning(this.getX() + (random.nextDouble() * 2.0 - 1.0) * 6.0,
                        this.getY() - 2.5,
                        this.getZ() + (random.nextDouble() * 2.0 - 1.0) * 6.0,
                        true);
            }
            setPhase(PHASE_ASCENDING);
        }
    }

    private void tickAscending() {
        Vec3 hover = this.target.add(0.0, HOVER_HEIGHT, 0.0);
        Vec3 toHover = hover.subtract(this.position());
        if (toHover.length() < 1.5 || this.tickCount - this.phaseStartTick > ASCEND_MAX_TICKS) {
            this.setDeltaMovement(Vec3.ZERO);
            setPhase(PHASE_FALLING);
            return;
        }
        double speed = Math.min(ASCEND_SPEED, 0.15 + (this.tickCount - this.phaseStartTick) * 0.08);
        this.setDeltaMovement(toHover.normalize().scale(speed));
        this.hasImpulse = true;
        this.move(MoverType.SELF, this.getDeltaMovement());
    }

    private void tickFalling() {
        double vy = this.tickCount - this.phaseStartTick <= 1
                ? -0.15
                : Math.max(this.getDeltaMovement().y - 0.04, -1.2);
        this.setDeltaMovement(0.0, vy, 0.0);
        this.hasImpulse = true;
        this.move(MoverType.SELF, this.getDeltaMovement());

        if ((this.tickCount - this.phaseStartTick) % 3 == 0) {
            int bolts = 1 + random.nextInt(2);
            for (int i = 0; i < bolts; i++) {
                spawnLightning(this.getX() + (random.nextDouble() * 2.0 - 1.0) * 6.0,
                        this.getY(),
                        this.getZ() + (random.nextDouble() * 2.0 - 1.0) * 6.0,
                        true);
            }
        }

        if (this.onGround() || this.verticalCollision) {
            landingImpact();
            setPhase(PHASE_GROUNDED);
        }
    }

    private void tickGrounded() {
        this.setDeltaMovement(Vec3.ZERO);
        int elapsed = this.tickCount - this.phaseStartTick;
        if (elapsed > 0 && elapsed % GROUND_PULSE_INTERVAL == 0) {
            groundPulse();
        }
        if (elapsed > 0 && elapsed % 30 == 0) {
            thunderFlash();
        }
        if (elapsed >= groundedTicks()) {
            finalBurst();
        }
    }

    private void thunderFlash() {
        int charge = getChargeLevel();
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER,
                2.0F, 0.8F + random.nextFloat() * 0.2F);
        double radius = 4.0 + 2.0 * charge;
        for (int i = 0; i < 5; i++) {
            double angle = Math.PI * 2.0 * i / 5.0;
            spawnLightning(this.getX() + Math.cos(angle) * radius,
                    this.getY(),
                    this.getZ() + Math.sin(angle) * radius,
                    true);
        }
    }

    private void landingImpact() {
        int charge = getChargeLevel();
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER,
                10000.0F, 0.6F + random.nextFloat() * 0.2F);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.BEACON_DEACTIVATE, SoundSource.WEATHER, 4.0F, 0.5F);
        this.level().explode(this, this.getX(), this.getY(), this.getZ(),
                2.5F * charge, Level.ExplosionInteraction.NONE);
        int bolts = 14 + random.nextInt(6);
        double radius = 5.0 + 3.0 * charge;
        for (int i = 0; i < bolts; i++) {
            double angle = Math.PI * 2.0 * i / bolts;
            spawnLightning(this.getX() + Math.cos(angle) * radius,
                    this.getY(),
                    this.getZ() + Math.sin(angle) * radius,
                    false);
        }
        spawnLightning(this.getX(), this.getY(), this.getZ(), false);
        // massive landing damage
        double damageRadius = 6.0 + 3.0 * charge;
        float damage = 10.0F + 5.0F * charge;
        Player owner = getOwner();
        for (LivingEntity victim : this.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(this.getX() - damageRadius, this.getY() - 2, this.getZ() - damageRadius,
                        this.getX() + damageRadius, this.getY() + 4, this.getZ() + damageRadius),
                e -> e != owner && e.isAlive())) {
            {
                    Player tbOwner = getOwner();
                    victim.hurt(tbOwner != null ? this.level().damageSources().indirectMagic(tbOwner, tbOwner)
                            : this.level().damageSources().magic(), damage);
                }
        }
        particleColumn(40);
    }

    private void particleColumn(int height) {
        if (this.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i <= height; i++) {
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        this.getX(), this.getY() + i, this.getZ(),
                        2, 0.3, 0.1, 0.3, 0.0);
                if (i % 2 == 0) {
                    serverLevel.sendParticles(ParticleTypes.END_ROD,
                            this.getX(), this.getY() + i, this.getZ(),
                            1, 0.15, 0.05, 0.15, 0.0);
                }
            }
        }
    }

    private void groundPulse() {
        int charge = getChargeLevel();
        double boltRadius = 5.0 + 3.0 * charge;
        int bolts = 6 + random.nextInt(4);
        int real = (bolts + 1) / 2;
        for (int i = 0; i < bolts; i++) {
            spawnLightning(this.getX() + (random.nextDouble() * 2.0 - 1.0) * boltRadius,
                    this.getY(),
                    this.getZ() + (random.nextDouble() * 2.0 - 1.0) * boltRadius,
                    i >= real);
        }

        double damageRadius = 6.0 + 3.0 * charge;
        float damage = 6.0F + 4.0F * charge;
        Player owner = getOwner();
        AABB area = new AABB(
                this.getX() - damageRadius, this.getY() - damageRadius, this.getZ() - damageRadius,
                this.getX() + damageRadius, this.getY() + damageRadius, this.getZ() + damageRadius);
        List<LivingEntity> victims = this.level().getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != owner && entity.isAlive());
        for (LivingEntity victim : victims) {
            {
                    Player tbOwner = getOwner();
                    victim.hurt(tbOwner != null ? this.level().damageSources().indirectMagic(tbOwner, tbOwner)
                            : this.level().damageSources().magic(), damage);
                }
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
        }

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER,
                2.0F, 0.7F + random.nextFloat() * 0.2F);
    }

    private void finalBurst() {
        int charge = getChargeLevel();
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER,
                10000.0F, 0.5F + random.nextFloat() * 0.2F);
        this.level().explode(this, this.getX(), this.getY(), this.getZ(),
                2.0F * charge, Level.ExplosionInteraction.NONE);
        int bolts = 14 + 4 * charge;
        double radius = 5.0 + 3.0 * charge;
        for (int i = 0; i < bolts; i++) {
            double angle = Math.PI * 2.0 * i / bolts;
            spawnLightning(this.getX() + Math.cos(angle) * radius,
                    this.getY(),
                    this.getZ() + Math.sin(angle) * radius,
                    false);
        }
        // final AoE damage
        Player owner = getOwner();
        for (LivingEntity victim : this.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(this.getX() - 8, this.getY() - 2, this.getZ() - 8,
                        this.getX() + 8, this.getY() + 4, this.getZ() + 8),
                e -> e != owner && e.isAlive())) {
            {
                    Player tbOwner = getOwner();
                    victim.hurt(tbOwner != null ? this.level().damageSources().indirectMagic(tbOwner, tbOwner)
                            : this.level().damageSources().magic(), 15.0F + 5.0F * charge);
                }
        }
        particleColumn(20);
        this.discard();
    }

    private void spawnLightning(double x, double y, double z, boolean visualOnly) {
        ServerPlayer cause = getOwner() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        if (visualOnly) {
            ElectroBolts.visual((ServerLevel) this.level(), x, y, z, cause);
        } else {
            ElectroBolts.strike((ServerLevel) this.level(), x, y, z, cause);
        }
    }

    private void spawnParticles() {
        int charge = getChargeLevel();
        double radius = 1.875 * charge;
        double cy = this.getY() + 1.0;
        switch (getPhase()) {
            case PHASE_FORMING -> {
                for (int i = 0; i < 16; i++) {
                    double theta = random.nextDouble() * Math.PI * 2.0;
                    double phi = Math.acos(2.0 * random.nextDouble() - 1.0);
                    double spawnR = radius + 1.5;
                    double sx = this.getX() + spawnR * Math.sin(phi) * Math.cos(theta);
                    double sy = cy + spawnR * Math.cos(phi);
                    double sz = this.getZ() + spawnR * Math.sin(phi) * Math.sin(theta);
                    this.level().addParticle(ParticleTypes.ELECTRIC_SPARK, sx, sy, sz,
                            (this.getX() - sx) * 0.12, (cy - sy) * 0.12, (this.getZ() - sz) * 0.12);
                }
                for (int i = 0; i < 3; i++) {
                    this.level().addParticle(ParticleTypes.CLOUD,
                            this.getX() + (random.nextDouble() - 0.5) * radius,
                            cy + radius + 1.0 + random.nextDouble(),
                            this.getZ() + (random.nextDouble() - 0.5) * radius,
                            0.0, 0.03, 0.0);
                    this.level().addParticle(ParticleTypes.SMOKE,
                            this.getX() + (random.nextDouble() - 0.5) * radius,
                            cy + 1.0,
                            this.getZ() + (random.nextDouble() - 0.5) * radius,
                            0.0, 0.05, 0.0);
                }
            }
            case PHASE_ASCENDING -> {
                for (int i = 0; i < 20; i++) {
                    this.level().addParticle(ParticleTypes.ELECTRIC_SPARK,
                            this.getX() + (random.nextDouble() - 0.5) * radius,
                            this.getY() - 1.0 - random.nextDouble() * 2.0,
                            this.getZ() + (random.nextDouble() - 0.5) * radius,
                            0.0, -0.15, 0.0);
                }
                this.level().addParticle(ParticleTypes.END_ROD,
                        this.getX(), cy, this.getZ(), 0.0, -0.3, 0.0);
            }
            case PHASE_GROUNDED -> {
                for (int i = 0; i < 20; i++) {
                    this.level().addParticle(ParticleTypes.ELECTRIC_SPARK,
                            this.getX() + (random.nextDouble() - 0.5) * radius,
                            this.getY() + 0.3,
                            this.getZ() + (random.nextDouble() - 0.5) * radius,
                            (random.nextDouble() - 0.5) * 0.1,
                            0.3 + random.nextDouble() * 0.4,
                            (random.nextDouble() - 0.5) * 0.1);
                }
                this.level().addParticle(ParticleTypes.END_ROD,
                        this.getX(), this.getY() + 1.0, this.getZ(),
                        0.0, 0.4, 0.0);
            }
            default -> {
                for (int i = 0; i < 16; i++) {
                    double theta = random.nextDouble() * Math.PI * 2.0;
                    double phi = Math.acos(2.0 * random.nextDouble() - 1.0);
                    this.level().addParticle(ParticleTypes.ELECTRIC_SPARK,
                            this.getX() + radius * Math.sin(phi) * Math.cos(theta),
                            cy + radius * Math.cos(phi),
                            this.getZ() + radius * Math.sin(phi) * Math.sin(theta),
                            0.0, 0.0, 0.0);
                }
                for (int i = 0; i < 8; i++) {
                    double angle = Math.PI * 2.0 * i / 8.0;
                    this.level().addParticle(ParticleTypes.ELECTRIC_SPARK,
                            this.getX() + Math.cos(angle) * radius * 1.5,
                            cy,
                            this.getZ() + Math.sin(angle) * radius * 1.5,
                            0.0, 0.1, 0.0);
                }
                for (int i = 0; i < 2; i++) {
                    this.level().addParticle(ParticleTypes.END_ROD,
                            this.getX(), cy, this.getZ(),
                            (random.nextDouble() - 0.5) * 0.3,
                            0.5 + random.nextDouble() * 0.5,
                            (random.nextDouble() - 0.5) * 0.3);
                }
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("ChargeLevel", getChargeLevel());
        tag.putInt("Phase", getPhase());
        tag.putInt("PhaseStartTick", this.phaseStartTick);
        tag.putDouble("TargetX", this.target.x);
        tag.putDouble("TargetY", this.target.y);
        tag.putDouble("TargetZ", this.target.z);
        if (this.ownerUUID != null) {
            tag.putUUID("Owner", this.ownerUUID);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setChargeLevel(tag.getInt("ChargeLevel"));
        setPhase(tag.getInt("Phase"));
        this.phaseStartTick = tag.getInt("PhaseStartTick");
        this.target = new Vec3(tag.getDouble("TargetX"), tag.getDouble("TargetY"), tag.getDouble("TargetZ"));
        if (tag.hasUUID("Owner")) {
            this.ownerUUID = tag.getUUID("Owner");
        }
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket(
            net.minecraft.server.level.ServerEntity serverEntity) {
        return new ClientboundAddEntityPacket(this, serverEntity);
    }
}
