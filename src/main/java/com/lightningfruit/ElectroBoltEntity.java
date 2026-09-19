package com.lightningfruit;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.Random;
import net.minecraft.world.phys.Vec3;

// custom electro bolt: own renderer (jagged glowing arc), never sets fire,
// strike damage is magic ("killed by magic")
public class ElectroBoltEntity extends Entity {
    private static final int MAX_LIFE = 14;
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> HOLY =
            net.minecraft.network.syncher.SynchedEntityData.defineId(ElectroBoltEntity.class,
                    net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

    public ElectroBoltEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static void strike(ServerLevel level, double x, double y, double z, @Nullable ServerPlayer cause) {
        strike(level, x, y, z, (LivingEntity) cause, false);
    }

    public static void strike(ServerLevel level, double x, double y, double z, @Nullable ServerPlayer cause,
                              boolean holy) {
        strike(level, x, y, z, (LivingEntity) cause, holy);
    }

    public static void strike(ServerLevel level, double x, double y, double z, @Nullable LivingEntity cause,
                              boolean holy) {
        spawn(level, x, y, z, holy);
        // powerful strike: 25 magic damage + knockback burst, no fire
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(x - 3.0, y - 1.0, z - 3.0, x + 3.0, y + 4.0, z + 3.0), LivingEntity::isAlive)) {
            if (entity != cause) {
                entity.hurt(cause != null ? level.damageSources().indirectMagic(cause, cause) : level.damageSources().magic(), 25.0F);
                Vec3 away = entity.position().subtract(new Vec3(x, y, z)).normalize().scale(1.2);
                entity.push(away.x, 0.5, away.z);
                entity.hurtMarked = true;
            }
        }
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                x, y + 1, z, 30, 2.0, 1.5, 2.0, 0.08);
    }

    public static void visual(ServerLevel level, double x, double y, double z) {
        visual(level, x, y, z, false);
    }

    public static void visual(ServerLevel level, double x, double y, double z, boolean holy) {
        spawn(level, x, y, z, holy);
    }

    public boolean isHoly() {
        return this.entityData.get(HOLY);
    }

    private static void spawn(ServerLevel level, double x, double y, double z, boolean holy) {
        ElectroBoltEntity bolt = new ElectroBoltEntity(ModEntities.ELECTRO_BOLT.get(), level);
        bolt.setPos(x, y, z);
        bolt.entityData.set(HOLY, holy);
        level.addFreshEntity(bolt);
        level.playSound(null, x, y, z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER,
                10000.0F, 0.8F + new Random().nextFloat() * 0.2F);
    }

    public int getBoltAge() {
        return this.tickCount;
    }

    public float getFade() {
        float age = this.tickCount;
        if (age < 2.0F) {
            return age / 2.0F;
        }
        return Math.max(0.0F, 1.0F - (age - 2.0F) / (MAX_LIFE - 2.0F));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.tickCount > MAX_LIFE) {
            this.discard();
        }
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
