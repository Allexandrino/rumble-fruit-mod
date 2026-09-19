package com.rumblefruit;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import java.util.Random;

// C skill: continuous lightning pillar from a cloud in the sky down to the ground
public class RumblePillarEntity extends Entity {
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> HOLY =
            net.minecraft.network.syncher.SynchedEntityData.defineId(RumblePillarEntity.class,
                    net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    public static final int MAX_LIFE = 30;
    public static final double PILLAR_HEIGHT = 60.0;
    public static final double CLOUD_Y = 58.0;

    private final Random random = new Random();

    public RumblePillarEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static void summon(ServerLevel level, double x, double y, double z) {
        summon(level, x, y, z, false);
    }

    public static void summon(ServerLevel level, double x, double y, double z, boolean holy) {
        RumblePillarEntity pillar = new RumblePillarEntity(ModEntities.LIGHTNING_PILLAR.get(), level);
        pillar.setPos(x, y, z);
        pillar.entityData.set(HOLY, holy);
        level.addFreshEntity(pillar);
        level.playSound(null, x, y, z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER,
                10000.0F, 0.5F + new Random().nextFloat() * 0.2F);
        level.playSound(null, x, y, z, SoundEvents.BEACON_DEACTIVATE, SoundSource.WEATHER, 4.0F, 0.5F);
    }

    public float getFade() {
        float age = this.tickCount;
        if (age < 3.0F) {
            return age / 3.0F;
        }
        return Math.max(0.0F, 1.0F - (age - 3.0F) / (MAX_LIFE - 3.0F));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            spawnCloudParticles();
        } else if (this.tickCount > MAX_LIFE) {
            this.discard();
        }
    }

    // blox-fruits style: dark thundercloud cap at the top of the pillar + constant discharge
    private void spawnCloudParticles() {
        // cloud cap: a wide flat blob of dark cloud/smoke at CLOUD_Y
        for (int i = 0; i < 25; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double r = 2.0 + random.nextDouble() * 8.0;
            this.level().addParticle(ParticleTypes.CLOUD,
                    this.getX() + Math.cos(angle) * r,
                    this.getY() + CLOUD_Y + (random.nextDouble() - 0.5) * 2.0,
                    this.getZ() + Math.sin(angle) * r,
                    0.0, -0.02, 0.0);
            this.level().addParticle(ParticleTypes.SMOKE,
                    this.getX() + Math.cos(angle) * r * 0.9,
                    this.getY() + CLOUD_Y - 0.8 + (random.nextDouble() - 0.5) * 1.5,
                    this.getZ() + Math.sin(angle) * r * 0.9,
                    0.0, -0.03, 0.0);
        }
        // sparks raining down from the cloud into the beam
        for (int i = 0; i < 8; i++) {
            this.level().addParticle(ParticleTypes.ELECTRIC_SPARK,
                    this.getX() + (random.nextDouble() - 0.5) * 3.0,
                    this.getY() + CLOUD_Y - random.nextDouble() * 6.0,
                    this.getZ() + (random.nextDouble() - 0.5) * 3.0,
                    0.0, -0.6, 0.0);
        }
        // ground discharge
        for (int i = 0; i < 10; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double r = 1.5 + random.nextDouble() * 4.0;
            this.level().addParticle(ParticleTypes.ELECTRIC_SPARK,
                    this.getX() + Math.cos(angle) * r,
                    this.getY() + 0.2,
                    this.getZ() + Math.sin(angle) * r,
                    0.0, 0.4, 0.0);
        }
    }

    public boolean isHoly() {
        return this.entityData.get(HOLY);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        builder.define(HOLY, false);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
