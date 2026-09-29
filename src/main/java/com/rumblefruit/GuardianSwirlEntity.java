package com.rumblefruit;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// a guardian swirl: one of four crystal shards circling the Fallen Exorcist's
// core. while any orbit, the core takes no damage. they yield only to a
// hero's own hand — shoot them all down, then break the core
public class GuardianSwirlEntity extends Entity {
    private static final int MAX_HP = 40;
    private static final double ORBIT_RADIUS = 10.0;
    private static final double ORBIT_HEIGHT = 25.0; // the face hovers here

    private int hp = MAX_HP;
    private int orbitIndex;
    private double orbitAngle;
    private FallenExorcistEntity boss; // runtime link, re-found after reload
    private int searchCooldown;

    public GuardianSwirlEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public void setOrbitIndex(int index) {
        this.orbitIndex = index;
        this.orbitAngle = index * Math.PI / 2.0;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean canBeHitByProjectile() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide) {
            return false;
        }
        // only a hero's hand breaks the ward — never the boss's own storms
        if (!(source.getEntity() instanceof Player)) {
            return false;
        }
        hp -= (int) Math.ceil(amount);
        if (hp > 0) {
            return true;
        }
        // the swirl shatters: violet burst, a crack of thunder, and every
        // watcher gets the pencil-sketch impact frame (combo code 40)
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(ModParticles.ELECTRO_GLOW.get(),
                    this.getX(), this.getY(), this.getZ(), 60, 1.0, 1.0, 1.0, 0.3);
            level.sendParticles(ModParticles.ELECTRO_SPARK.get(),
                    this.getX(), this.getY(), this.getZ(), 40, 1.5, 1.5, 1.5, 0.2);
            level.playSound(null, this.getX(), this.getY(), this.getZ(),
                    net.minecraft.sounds.SoundEvents.GLASS_BREAK,
                    net.minecraft.sounds.SoundSource.HOSTILE, 3.0F, 0.6F);
            for (var p : level.players()) {
                p.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        "rumblefruit.swirl_down").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE), false);
            }
            if (boss != null) {
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntity(
                        boss, new CombatAnimPacket(boss.getUUID(), 40));
            }
        }
        this.discard();
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        if (boss == null || !boss.isAlive()) {
            if (--searchCooldown > 0) {
                return;
            }
            searchCooldown = 20;
            var found = this.level().getEntitiesOfClass(FallenExorcistEntity.class,
                    this.getBoundingBox().inflate(80.0));
            if (found.isEmpty()) {
                this.discard(); // the throne is empty — the ward dissolves
                return;
            }
            boss = found.get(0);
        }
        // circle the core, each shard a quarter-turn apart, slowly bobbing
        orbitAngle += 0.022;
        double bob = Math.sin((this.tickCount + orbitIndex * 17) * 0.07) * 1.6;
        Vec3 center = boss.position().add(0.0, ORBIT_HEIGHT, 0.0);
        this.setPos(center.x + Math.cos(orbitAngle) * ORBIT_RADIUS,
                center.y + bob,
                center.z + Math.sin(orbitAngle) * ORBIT_RADIUS);
        // the shard spins and sheds violet dust
        if (this.level() instanceof ServerLevel level && this.tickCount % 4 == 0) {
            level.sendParticles(ModParticles.ELECTRO_GLOW.get(),
                    this.getX(), this.getY() + 0.6, this.getZ(), 1, 0.15, 0.15, 0.15, 0.0);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        hp = tag.getInt("rumblefruit_hp");
        orbitIndex = tag.getInt("rumblefruit_orbit");
        orbitAngle = tag.getDouble("rumblefruit_angle");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("rumblefruit_hp", hp);
        tag.putInt("rumblefruit_orbit", orbitIndex);
        tag.putDouble("rumblefruit_angle", orbitAngle);
    }
}
