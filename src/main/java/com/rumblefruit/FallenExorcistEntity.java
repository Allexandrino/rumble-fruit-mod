package com.rumblefruit;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

// the Fallen Exorcist: an insanely strong elite enemy — a corrupted angel hunter.
// huge health pool, fast, heavy melee, calls golden lightning down on its target.
public class FallenExorcistEntity extends Monster {

    public FallenExorcistEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 60;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 400.0)
                .add(Attributes.ATTACK_DAMAGE, 16.0)
                .add(Attributes.MOVEMENT_SPEED, 0.38)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.ARMOR_TOUGHNESS, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.4, true));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
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
        // golden lightning on the target every 4 seconds (insane pressure)
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel
                && this.getTarget() != null && this.tickCount % 80 == 0) {
            LivingEntity target = this.getTarget();
            if (target.isAlive() && this.distanceTo(target) < 28.0F) {
                ElectroBoltEntity.strike(serverLevel, target.getX(), target.getY(), target.getZ(), this, true);
                this.playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 2.0F, 0.6F);
            }
        }
        // roar when acquiring a target
        if (!this.level().isClientSide && this.getTarget() != null && this.tickCount % 200 == 0) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.5F, 0.7F);
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel serverLevel, net.minecraft.world.damagesource.DamageSource source,
                                       boolean recentlyHit) {
        super.dropCustomDeathLoot(serverLevel, source, recentlyHit);
        // drops the lightning fruit so the boss is worth hunting
        this.spawnAtLocation(new net.minecraft.world.item.ItemStack(RumbleFruitMod.ELECTRO_APPLE.get(),
                1 + this.random.nextInt(2)));
    }
}
