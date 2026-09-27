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

// the Fallen Exorcist: a 50-block colossus assembled from hundreds of cubes,
// the corrupted angel hunter reigning over the vault of the exorcist realm.
// stationary, all-seeing, armed with a 25-move arsenal (see ExorcistAttacks);
// below half health it attacks twice as fast.
public class FallenExorcistEntity extends Monster {

    private int attackCooldown = 70;
    private int attackIndex = 0;
    private int lastPhase = 1;
    // telegraph state: the marked strike zone and when the blow lands
    private int pendingAttack = -1;
    private Vec3 pendingPos;
    private long fireAt;
    // the hall this throne reigns over — used to re-count crystal wards
    private net.minecraft.core.BlockPos home;
    private boolean homeScanned;

    public FallenExorcistEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 300;
    }

    public void setHome(net.minecraft.core.BlockPos home) {
        this.home = home;
    }

    // the exorcist never despawns — it reigns until slain
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (home != null) {
            tag.putLong("rumblefruit_home", home.asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("rumblefruit_home")) {
            home = net.minecraft.core.BlockPos.of(tag.getLong("rumblefruit_home"));
        }
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
        // the guardian crystals shield the throne: no damage while any burn
        if (GuardianCrystals.alive() > 0) {
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
        // first tick in a fresh JVM: the crystal counter is static memory,
        // the crystal BLOCKS are the world — recount the wards still standing
        if (!homeScanned) {
            homeScanned = true;
            if (home != null && GuardianCrystals.alive() == 0) {
                int standing = 0;
                for (int[] off : MeteorDungeon.CRYSTAL_OFFSETS) {
                    if (serverLevel.getBlockState(home.offset(off[0], off[1], off[2]))
                            .is(ModBlocks.GUARDIAN_CRYSTAL.get())) {
                        standing++;
                    }
                }
                if (standing > 0) {
                    GuardianCrystals.arm(standing);
                }
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
