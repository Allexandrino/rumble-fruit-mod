package com.rumblefruit;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

import java.util.Random;

// epic electric projectile fired from the electro bow: flies fast with a lightning
// trail, and on impact calls down lightning + AoE magic damage
public class ElectroArrowEntity extends AbstractArrow {
    private final Random random = new Random();
    private boolean pillarOnImpact = false;
    private int pierceRemaining = 0;

    public void setPiercing(int count) {
        this.pierceRemaining = count;
    }

    public ElectroArrowEntity(EntityType<? extends ElectroArrowEntity> type, Level level) {
        super(type, level);
        // normal arrow ballistics (gravity arc) like a regular bow, but electric on impact
        this.pickup = AbstractArrow.Pickup.DISALLOWED;
    }

    public ElectroArrowEntity(Level level, LivingEntity owner) {
        super(ModEntities.ELECTRO_ARROW.get(), owner, level,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW), null);
        this.pickup = AbstractArrow.Pickup.DISALLOWED;
    }

    @Override
    protected net.minecraft.world.item.ItemStack getDefaultPickupItem() {
        return new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW);
    }

    public void setPillarOnImpact(boolean value) {
        this.pillarOnImpact = value;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            for (int i = 0; i < 3; i++) {
                this.level().addParticle(ModParticles.ELECTRO_SPARK.get(),
                        this.getX() + (random.nextDouble() - 0.5) * 0.2,
                        this.getY() + (random.nextDouble() - 0.5) * 0.2,
                        this.getZ() + (random.nextDouble() - 0.5) * 0.2,
                        0.0, 0.0, 0.0);
            }
            this.level().addParticle(ModParticles.ELECTRO_GLOW.get(), this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
        }
        if (this.tickCount > 60) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!this.level().isClientSide && result.getEntity() instanceof LivingEntity target) {
            ServerLevel level = (ServerLevel) this.level();
            target.hurt(this.getOwner() != null ? level.damageSources().indirectMagic(this.getOwner(), this.getOwner()) : level.damageSources().magic(), 20.0F);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
            ElectroBolts.strike(level, target.getX(), target.getY(), target.getZ(),
                    this.getOwner() instanceof net.minecraft.server.level.ServerPlayer sp ? sp : null);
        }
        if (this.pierceRemaining > 0) {
            this.pierceRemaining--;
        } else {
            this.discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (!this.level().isClientSide) {
            ServerLevel level = (ServerLevel) this.level();
            var pos = result.getLocation();
            net.minecraft.server.level.ServerPlayer sp2 =
                    this.getOwner() instanceof net.minecraft.server.level.ServerPlayer sp ? sp : null;
            ElectroBolts.strike(level, pos.x, pos.y, pos.z, sp2);
            if (this.pillarOnImpact) {
                // thunder arrow: a golden lightning strike at the impact point
                ElectroBoltEntity.strike(level, pos.x, pos.y, pos.z, sp2,
                        sp2 != null && WingsData.isActive(sp2.getUUID()));
            }
            // small AoE on block impact
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(pos.x - 3, pos.y - 3, pos.z - 3, pos.x + 3, pos.y + 3, pos.z + 3),
                    e -> e != this.getOwner() && e.isAlive())) {
                entity.hurt(this.getOwner() != null ? level.damageSources().indirectMagic(this.getOwner(), this.getOwner()) : level.damageSources().magic(), 15.0F);
            }
            level.sendParticles(ModParticles.ELECTRO_SPARK.get(), pos.x, pos.y + 0.5, pos.z, 30, 1.5, 1.0, 1.5, 0.05);
            level.playSound(null, pos.x, pos.y, pos.z, ModSounds.ELECTRO_ZAP.get(), SoundSource.WEATHER, 2.0F, 1.0F);
        }
        this.discard();
    }

    @Override
    protected ItemStack getPickupItem() {
        return ItemStack.EMPTY;
    }
}
