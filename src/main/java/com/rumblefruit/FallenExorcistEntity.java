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
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

// the Fallen Exorcist: a 50-block colossus assembled from hundreds of cubes,
// the corrupted angel hunter reigning over the vault of the exorcist realm.
// stationary, all-seeing, armed with a 25-move arsenal (see ExorcistAttacks);
// below half health it attacks twice as fast.
public class FallenExorcistEntity extends Monster {

    private int attackCooldown = 70;
    private int attackIndex = 0;

    public FallenExorcistEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 300;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1500.0)
                .add(Attributes.ATTACK_DAMAGE, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0) // it does not chase — it reigns
                .add(Attributes.ARMOR, 20.0)
                .add(Attributes.ARMOR_TOUGHNESS, 10.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 96.0)
                .add(Attributes.SCALE, 25.0); // 0.8 x 25 = 20 wide, 2.0 x 25 = 50 blocks tall
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 64.0F));
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
        // the colossus bleeds fury: below half health it attacks twice as fast
        int period = this.getHealth() < this.getMaxHealth() * 0.5F ? 40 : 70;
        if (--attackCooldown > 0) {
            return;
        }
        attackCooldown = period;
        ExorcistAttacks.perform(attackIndex++, serverLevel, this, target);
        if (attackIndex % 5 == 0) {
            this.playSound(SoundEvents.ENDER_DRAGON_GROWL, 2.0F, 0.6F);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // the cube shell shrugs off a third of everything
        return super.hurt(source, amount * 0.67F);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel serverLevel, DamageSource source,
                                       boolean recentlyHit) {
        super.dropCustomDeathLoot(serverLevel, source, recentlyHit);
        // drops the lightning fruit so the boss is worth hunting
        this.spawnAtLocation(new net.minecraft.world.item.ItemStack(RumbleFruitMod.ELECTRO_APPLE.get(),
                1 + this.random.nextInt(2)));
    }
}
