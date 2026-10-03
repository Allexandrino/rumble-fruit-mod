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
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

// the Fallen Exorcist: a colossal horned face looming out of the abyss pit of
// its realm (see exorcist.geo.json). stationary, all-seeing, armed with a 25-move
// arsenal (see ExorcistAttacks); below half health it attacks twice as fast.
// its core is warded by four guardian swirls — break them, then the core
public class FallenExorcistEntity extends Monster implements GeoEntity {

    // GeckoLib: looping idle menace + triggered attack lunge
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.exorcist.idle");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.exorcist.attack");
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0,
                state -> state.setAndContinue(IDLE)));
        controllers.add(new AnimationController<>(this, "attack", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    // every wind-up swing (telegraphed or striking) lunges the whole visage
    // forward on the client too — swing packets reach clients for free
    @Override
    public void swing(net.minecraft.world.InteractionHand hand) {
        super.swing(hand);
        if (level().isClientSide) {
            triggerAnim("attack", "attack");
        }
    }

    private int attackCooldown = 70;
    private int attackIndex = 0;
    private int lastPhase = 1;
    // telegraph state: the marked strike zone and when the blow lands
    private int pendingAttack = -1;
    private Vec3 pendingPos;
    private long fireAt;
    // the ward: guardian swirls circling the core (recounted from the world)
    private int swirlCount = 4;
    private boolean vulnerabilityAnnounced;

    public FallenExorcistEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 300;
    }

    // the exorcist never despawns — it reigns until slain
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    public int getSwirlCount() {
        return swirlCount;
    }

    // the boss bar: a dark-red notched bar while anyone fights in the cavern
    private final net.minecraft.server.level.ServerBossEvent bossBar = new net.minecraft.server.level.ServerBossEvent(
            net.minecraft.network.chat.Component.translatable("entity.rumblefruit.fallen_exorcist"),
            net.minecraft.world.BossEvent.BossBarColor.RED,
            net.minecraft.world.BossEvent.BossBarOverlay.NOTCHED_10);

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // the exorcist yields only to a hero's own hand: no lava, no walls,
        // no stray bolts (its own arsenal included) — /kill and the void excepted
        if (source.is(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL)
                || source.is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD)) {
            return super.hurt(source, amount);
        }
        if (!(source.getEntity() instanceof net.minecraft.world.entity.player.Player p)) {
            return false;
        }
        // the guardian swirls ward the core: no damage while any orbit
        if (swirlCount > 0) {
            p.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "rumblefruit.exorcist_shielded").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE), true);
            return false;
        }
        return super.hurt(source, amount * 0.67F);
    }

    @Override
    public void die(DamageSource source) {
        bossBar.removeAllPlayers();
        super.die(source);
    }

    // the bar follows the entity tracker: shown when the boss is tracked,
    // dropped on dimension change, chunk unload, death — no lingering ghosts
    @Override
    public void startSeenByPlayer(net.minecraft.server.level.ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(net.minecraft.server.level.ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    @Override
    public void remove(RemovalReason reason) {
        // death, /kill, chunk unload — the bar must never linger
        bossBar.removeAllPlayers();
        super.remove(reason);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1024.0) // vanilla clamps max health at 1024
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
        // the ward is made of living shards, not memory: recount the swirls
        // still circling the core — reloads, stray /kills, nothing desyncs
        if (this.tickCount % 20 == 0) {
            swirlCount = serverLevel.getEntitiesOfClass(GuardianSwirlEntity.class,
                    this.getBoundingBox().inflate(60.0)).size();
            if (swirlCount == 0 && !vulnerabilityAnnounced) {
                vulnerabilityAnnounced = true;
                this.playSound(SoundEvents.ENDER_DRAGON_GROWL, 3.0F, 1.2F);
                serverLevel.sendParticles(ModParticles.ELECTRO_GLOW.get(),
                        this.getX(), this.getY() + 25.0, this.getZ(), 120, 4.0, 4.0, 4.0, 0.2);
                for (var p : serverLevel.players()) {
                    p.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                            "rumblefruit.core_exposed").withStyle(net.minecraft.ChatFormatting.GOLD), false);
                }
            }
        }
        // one throne, one exorcist: an older twin from a stale save retires
        if (this.tickCount % 100 == 7) {
            for (var twin : serverLevel.getEntitiesOfClass(FallenExorcistEntity.class,
                    this.getBoundingBox().inflate(80.0), e -> e != this && e.isAlive())) {
                if (this.tickCount < twin.tickCount) {
                    this.discard();
                    return;
                }
                twin.discard();
            }
        }
        LivingEntity target = this.getTarget();
        bossBar.setProgress(this.getHealth() / this.getMaxHealth());
        // drop watchers who left this dimension (tracker misses some edge cases)
        for (var p : java.util.List.copyOf(bossBar.getPlayers())) {
            if (p.level() != this.level()) {
                bossBar.removePlayer(p);
            }
        }
        // the abyss breathes: violet light and black smoke seep out of the pit
        long breath = level().getGameTime();
        if (breath % 6 == 0) {
            double a = this.random.nextDouble() * Math.PI * 2.0;
            double r = 3.0 + this.random.nextDouble() * 7.0;
            serverLevel.sendParticles(ModParticles.ELECTRO_GLOW.get(),
                    this.getX() + Math.cos(a) * r, this.getY() + 2.0 + this.random.nextDouble() * 10.0,
                    this.getZ() + Math.sin(a) * r, 1, 0.0, 0.35, 0.0, 0.0);
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                    this.getX() + Math.cos(a + 2.0) * r, this.getY() + 1.0,
                    this.getZ() + Math.sin(a + 2.0) * r, 1, 0.0, 0.3, 0.0, 0.01);
        }
        if (target == null || !target.isAlive()) {
            return;
        }
        // seven stages: every stage unlocks more of the arsenal and the
        // colossus speeds up; stage changes roar and flash across the cave
        int phase = ExorcistAttacks.phaseFor(this.getHealth(), this.getMaxHealth());
        if (phase > lastPhase) {
            lastPhase = phase;
            this.playSound(SoundEvents.ENDER_DRAGON_GROWL, 3.0F, 0.5F);
            serverLevel.sendParticles(ModParticles.ELECTRO_GLOW.get(),
                    this.getX(), this.getY() + 20.0, this.getZ(), 150, 6.0, 8.0, 6.0, 0.15);
            for (var p : serverLevel.players()) {
                p.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        "rumblefruit.exorcist_phase", phase).withStyle(net.minecraft.ChatFormatting.DARK_RED), false);
            }
        }
        int period = Math.max(18, 75 - phase * 8);
        long now = level().getGameTime();
        // telegraphed attacks: the exorcist winds up, marks the strike zone,
        // and only then the blow lands — dodge the mark and you dodge the hit
        if (pendingAttack >= 0) {
            if (pendingPos != null && now < fireAt) {
                // the strike zone burns while the exorcist winds up
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4.0 + now * 0.2;
                    serverLevel.sendParticles(ModParticles.ELECTRO_SPARK.get(),
                            pendingPos.x + Math.cos(a) * 2.2, pendingPos.y + 0.3,
                            pendingPos.z + Math.sin(a) * 2.2, 1, 0.0, 0.1, 0.0, 0.0);
                }
                serverLevel.sendParticles(ModParticles.ELECTRO_GLOW.get(),
                        pendingPos.x, pendingPos.y + 0.4, pendingPos.z, 3, 0.8, 0.2, 0.8, 0.0);
                return;
            }
            this.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true); // the strike pose
            ExorcistAttacks.perform(pendingAttack, serverLevel, this, pendingPos);
            pendingAttack = -1;
            pendingPos = null;
            attackCooldown = period;
            return;
        }
        if (--attackCooldown > 0) {
            return;
        }
        // wind up: the pose starts, the zone gets marked, the hit lands later
        this.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        pendingAttack = attackIndex++ % ExorcistAttacks.maxAttackForPhase(phase);
        pendingPos = target.position();
        fireAt = now + 24;
        if (attackIndex % 5 == 0) {
            this.playSound(SoundEvents.ENDER_DRAGON_GROWL, 2.0F, 0.6F);
        }
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
