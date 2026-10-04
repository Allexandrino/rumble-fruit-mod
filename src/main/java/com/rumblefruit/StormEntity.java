package com.rumblefruit;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Random;
import java.util.UUID;

// invisible server-side timer entity: mode 0 = thunderstorm (X), mode 1 = sky judgement (C)
public class StormEntity extends Entity {
    public static final int MODE_STORM = 0;
    public static final int MODE_JUDGEMENT = 1;

    private static final int STORM_TICKS = 60;
    private static final int JUDGEMENT_WARN_TICKS = 20;

    private int mode = MODE_STORM;
    private Vec3 target = Vec3.ZERO;
    private UUID ownerUuid;
    private final Random random = new Random();

    public StormEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static StormEntity storm(ServerLevel level, Vec3 target, ServerPlayer owner) {
        return spawn(level, target, owner, MODE_STORM);
    }

    public static StormEntity judgement(ServerLevel level, Vec3 target, ServerPlayer owner) {
        return spawn(level, target, owner, MODE_JUDGEMENT);
    }

    private static StormEntity spawn(ServerLevel level, Vec3 target, ServerPlayer owner, int mode) {
        StormEntity entity = new StormEntity(ModEntities.STORM.get(), level);
        entity.mode = mode;
        entity.target = target;
        entity.ownerUuid = owner.getUUID();
        entity.setPos(target.x, target.y, target.z);
        level.addFreshEntity(entity);
        return entity;
    }

    private ServerPlayer owner() {
        if (this.ownerUuid == null || !(this.level() instanceof ServerLevel level)) {
            return null;
        }
        return level.getServer().getPlayerList().getPlayer(this.ownerUuid);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (this.mode == MODE_STORM) {
            tickStorm(level);
        } else {
            tickJudgement(level);
        }
    }

    private void tickStorm(ServerLevel level) {
        if (this.tickCount > STORM_TICKS) {
            this.discard();
            return;
        }
        // strike every 4 ticks for a denser storm
        if (this.tickCount % 4 == 0) {
            double x = this.target.x + (random.nextDouble() * 2.0 - 1.0) * 8.0;
            double z = this.target.z + (random.nextDouble() * 2.0 - 1.0) * 8.0;
            ElectroBolts.strike(level, x, this.target.y, z, owner());
        }
        if (this.tickCount % 3 == 0) {
            level.sendParticles(ModParticles.ELECTRO_CLOUD.get(),
                    this.target.x + (random.nextDouble() - 0.5) * 12.0,
                    this.target.y + 12.0,
                    this.target.z + (random.nextDouble() - 0.5) * 12.0,
                    1, 0.05, 0.0, 0.05, 0.0);
        }
    }

    private void tickJudgement(ServerLevel level) {
        if (this.tickCount < JUDGEMENT_WARN_TICKS) {
            // warning: vertical END_ROD column at the target
            for (int i = 0; i < 3; i++) {
                level.sendParticles(ModParticles.ELECTRO_GLOW.get(),
                        this.target.x, this.target.y + random.nextDouble() * 30.0, this.target.z,
                        1, 0.2, 0.0, 0.2, 0.0);
            }
            return;
        }
        ServerPlayer owner = owner();
        for (int i = 0; i < 3; i++) {
            ElectroBolts.strike(level, this.target.x + (random.nextDouble() - 0.5) * 1.5,
                    this.target.y, this.target.z + (random.nextDouble() - 0.5) * 1.5, owner);
        }
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(this.target, this.target).inflate(5.0),
                e -> e != owner && e.isAlive())) {
            {
                net.minecraft.world.entity.player.Player stormOwner = this.ownerUuid != null
                        ? level.getPlayerByUUID(this.ownerUuid) : null;
                entity.hurt(stormOwner != null ? level.damageSources().indirectMagic(stormOwner, stormOwner)
                        : level.damageSources().magic(), 15.0F);
            }
            entity.push(0.0, 1.2, 0.0);
            entity.hurtMarked = true;
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 4));
        }
        level.sendParticles(ModParticles.ELECTRO_SPARK.get(), this.target.x, this.target.y + 1.0, this.target.z,
                40, 3.0, 1.0, 3.0, 0.05);
        this.discard();
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
