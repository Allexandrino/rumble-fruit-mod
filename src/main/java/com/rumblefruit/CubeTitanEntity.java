package com.rumblefruit;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

// the Cube Titan: a colossal golem assembled from dozens of floating cubes.
// guards the meteor dungeon — the only way out is through it. attacks from a
// 25-move arsenal (see CubeTitanAttacks), gets faster as it bleeds.
public class CubeTitanEntity extends Monster {

    private int attackCooldown = 70;
    private int attackIndex = 0;

    public CubeTitanEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 200;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 800.0)
                .add(Attributes.ATTACK_DAMAGE, 24.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.ARMOR, 16.0)
                .add(Attributes.ARMOR_TOUGHNESS, 10.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 64.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 32.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        // the titan bleeds fury: below half health it attacks twice as fast
        int period = this.getHealth() < this.getMaxHealth() * 0.5F ? 40 : 70;
        if (--attackCooldown > 0) {
            return;
        }
        attackCooldown = period;
        CubeTitanAttacks.perform(attackIndex++, serverLevel, this, target);
        if (attackIndex % 5 == 0) {
            this.playSound(SoundEvents.ENDER_DRAGON_GROWL, 2.0F, 0.6F);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // the cube shell shrugs off a third of everything
        return super.hurt(source, amount * 0.67F);
    }
}
