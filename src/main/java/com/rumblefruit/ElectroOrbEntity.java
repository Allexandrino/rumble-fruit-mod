package com.rumblefruit;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

// Z skill: small straight-flying energy orb, no gravity, ~30 blocks range
// Z3 (dragon mode): bigger, faster, more damage
public class ElectroOrbEntity extends ThrowableItemProjectile {
    private static final int MAX_TICKS = 20; // 1.5 blocks/tick * 20 = 30 blocks
    private static final int MAX_TICKS_DRAGON = 30; // longer range for dragon
    private final Random random = new Random();
    private boolean dragonMode = false;

    public ElectroOrbEntity(EntityType<? extends ElectroOrbEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public ElectroOrbEntity(Level level, LivingEntity owner) {
        super(ModEntities.ELECTRO_ORB.get(), owner, level);
        this.setNoGravity(true);
    }

    public void setDragonMode(boolean dragon) {
        this.dragonMode = dragon;
    }

    @Override
    protected Item getDefaultItem() {
        return RumbleFruitMod.ELECTRO_ORB_ITEM.get();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            int particleCount = this.dragonMode ? 6 : 3;
            double spread = this.dragonMode ? 0.5 : 0.3;
            for (int i = 0; i < particleCount; i++) {
                this.level().addParticle(ModParticles.ELECTRO_SPARK.get(),
                        this.getX() + (random.nextDouble() - 0.5) * spread,
                        this.getY() + (random.nextDouble() - 0.5) * spread,
                        this.getZ() + (random.nextDouble() - 0.5) * spread,
                        0.0, 0.0, 0.0);
            }
            if (this.dragonMode) {
                // dragon trail: bigger sparks
                this.level().addParticle(ModParticles.ELECTRO_GLOW.get(),
                        this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            }
        } else if (this.tickCount > (this.dragonMode ? MAX_TICKS_DRAGON : MAX_TICKS)) {
            detonate(this.position());
        }
    }

    @Override
    protected void onHit(HitResult result) {
        if (!this.level().isClientSide && !this.isRemoved()) {
            detonate(result.getLocation());
        }
        super.onHit(result);
    }

    private void detonate(Vec3 point) {
        ServerLevel level = (ServerLevel) this.level();
        double radius = this.dragonMode ? 6.0 : 4.0;
        float damage = this.dragonMode ? 30.0F : 20.0F;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(point, point).inflate(radius),
                e -> e != this.getOwner() && e.isAlive())) {
            entity.hurt(this.getOwner() != null ? level.damageSources().indirectMagic(this.getOwner(), this.getOwner()) : level.damageSources().magic(), damage);
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2));
            Vec3 pull = point.subtract(entity.position()).normalize().scale(this.dragonMode ? 1.5 : 1.2);
            entity.push(pull.x, pull.y * 0.3 + 0.3, pull.z);
            entity.hurtMarked = true;
        }
        level.sendParticles(ModParticles.ELECTRO_SPARK.get(), point.x, point.y, point.z,
                this.dragonMode ? 60 : 40, 2.0, 2.0, 2.0, 0.08);
        level.playSound(null, point.x, point.y, point.z,
                ModSounds.ELECTRO_ZAP.get(), SoundSource.PLAYERS, 2.0F, 0.8F);
        this.discard();
    }
}
