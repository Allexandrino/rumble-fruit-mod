package com.rumblefruit;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// a slab of the world itself, torn out by the terra-rip skill (G): it rises
// from the crater trailing dust, drifts over the caster's head like a loaded
// catapult, then hurls at wherever the caster is looking and detonates in a
// shower of real debris. what you throw is what stood there — the chunk
// carries the actual blocks it ripped out.
public class EarthChunkEntity extends Entity implements IEntityWithComplexSpawn {
    public static final int GRID = 27; // 3x3x3, air for the gaps
    private static final int RISE_TICKS = 12;
    private static final int HOVER_TICKS = 16;
    private static final int HURL_TICKS_MAX = 45;
    private static final double HURL_SPEED = 1.6;

    private final List<BlockState> states = new ArrayList<>();
    private UUID ownerId;
    private int elementId = 0;
    private int phase = 0; // 0 = rise, 1 = hover over the caster, 2 = hurl
    private int phaseTicks = 0;
    private Vec3 riseFrom;
    private Vec3 velocity = Vec3.ZERO;

    public EarthChunkEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public List<BlockState> states() {
        return states;
    }

    // the material the slab sheds as dust: the first solid block it carries
    private BlockState dustState() {
        for (BlockState state : states) {
            if (!state.isAir()) {
                return state;
            }
        }
        return Blocks.DIRT.defaultBlockState();
    }

    public static EarthChunkEntity spawn(ServerLevel level, ServerPlayer owner, Vec3 from,
                                         List<BlockState> states, int elementId) {
        EarthChunkEntity chunk = new EarthChunkEntity(ModEntities.EARTH_CHUNK.get(), level);
        chunk.ownerId = owner.getUUID();
        chunk.elementId = elementId;
        chunk.states.addAll(states);
        chunk.riseFrom = from;
        chunk.setPos(from.x, from.y, from.z);
        level.addFreshEntity(chunk);
        return chunk;
    }

    private ServerPlayer owner() {
        if (ownerId != null && level() instanceof ServerLevel serverLevel
                && serverLevel.getEntity(ownerId) instanceof ServerPlayer player) {
            return player;
        }
        return null;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        ServerLevel level = (ServerLevel) level();
        phaseTicks++;
        switch (phase) {
            case 0 -> tickRise(level);
            case 1 -> tickHover(level);
            case 2 -> tickHurl(level);
            default -> discard();
        }
    }

    // the slab tears free: climbs out of the crater, shaking off dirt
    private void tickRise(ServerLevel level) {
        double k = Math.min(1.0, phaseTicks / (double) RISE_TICKS);
        setPos(riseFrom.x, riseFrom.y + k * 2.4, riseFrom.z);
        if (phaseTicks % 2 == 0 && !states.isEmpty()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, dustState()),
                    getX(), getY() + 0.5, getZ(), 12, 0.9, 0.6, 0.9, 0.15);
        }
        if (phaseTicks >= RISE_TICKS) {
            phase = 1;
            phaseTicks = 0;
        }
    }

    // the loaded catapult: the slab drifts above the caster's head and waits
    private void tickHover(ServerLevel level) {
        ServerPlayer owner = owner();
        if (owner == null) {
            slam(level); // the caster is gone — drop it where it hangs
            return;
        }
        Vec3 anchor = owner.getEyePosition().add(0.0, 2.3, 0.0);
        setPos(getX() + (anchor.x - getX()) * 0.3,
                getY() + (anchor.y - getY()) * 0.3,
                getZ() + (anchor.z - getZ()) * 0.3);
        level.sendParticles(Element.byId(elementId).spark(),
                getX(), getY() + 1.0, getZ(), 4, 0.8, 0.4, 0.8, 0.02);
        if (phaseTicks >= HOVER_TICKS) {
            // hurl where the caster is looking RIGHT NOW
            Vec3 target = SkillExecutor.rayTracePublic(owner, 50.0);
            Vec3 dir = target.subtract(position());
            velocity = dir.length() < 0.5 ? new Vec3(0.0, -1.0, 0.0)
                    : dir.normalize().scale(HURL_SPEED);
            phase = 2;
            phaseTicks = 0;
            level.playSound(null, getX(), getY(), getZ(),
                    SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 1.5F, 0.5F);
        }
    }

    // the throw: a spinning slab of earth screaming toward the mark
    private void tickHurl(ServerLevel level) {
        setPos(getX() + velocity.x, getY() + velocity.y, getZ() + velocity.z);
        if (phaseTicks % 2 == 0) {
            level.sendParticles(ParticleTypes.CLOUD,
                    getX(), getY() + 0.5, getZ(), 3, 0.7, 0.7, 0.7, 0.01);
        }
        boolean hitWall = level().getBlockState(blockPosition()).isSolid();
        if (hitWall || phaseTicks >= HURL_TICKS_MAX) {
            slam(level);
        }
    }

    // impact: area damage + the element's rider, a real secondary crater and
    // the surviving blocks scattering as physical debris
    private void slam(ServerLevel level) {
        ServerPlayer owner = owner();
        float damage = 26.0F;
        boolean empowered = false;
        if (owner != null) {
            empowered = WingsData.isActive(owner.getUUID());
            if (empowered) {
                damage = 38.0F;
            }
            // the caster never eats his own landslide
            owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 6, 4, false, false));
        }
        double radius = 5.5;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(radius),
                e -> e != owner && e.isAlive())) {
            double dist = e.position().distanceTo(position());
            if (dist > radius) {
                continue;
            }
            float scaled = damage * (float) (1.0 - dist / (radius * 1.6));
            if (owner != null) {
                e.hurt(level.damageSources().indirectMagic(owner, owner), scaled);
                Element.byId(elementId).applyRider(e, owner, empowered);
            } else {
                e.hurt(level.damageSources().generic(), scaled);
            }
            Vec3 away = e.position().subtract(position()).multiply(1.0, 0.0, 1.0)
                    .normalize().scale(1.4);
            e.push(away.x, 0.9, away.z);
            e.hurtMarked = true;
        }
        // the slab shatters: real blocks fly out as debris
        int debris = 0;
        for (BlockState state : states) {
            if (state.isAir() || debris >= 6) {
                continue;
            }
            debris++;
            FallingBlockEntity block = FallingBlockEntity.fall(level, blockPosition().above(), state);
            block.setDeltaMovement((random.nextDouble() - 0.5) * 0.7,
                    0.4 + random.nextDouble() * 0.4, (random.nextDouble() - 0.5) * 0.7);
            block.hurtMarked = true;
        }
        if (!states.isEmpty()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, dustState()),
                    getX(), getY() + 0.5, getZ(), 200, 2.5, 1.5, 2.5, 0.3);
        }
        level.sendParticles(Element.byId(elementId).spark(),
                getX(), getY() + 0.8, getZ(), 80, 2.5, 1.2, 2.5, 0.08);
        level.explode(null, getX(), getY(), getZ(), 2.0F, Level.ExplosionInteraction.BLOCK);
        // БАМ: the ground SPLITS around the impact — real fissures crawl out
        TerraSkills.crackEarth(level, blockPosition(), 6, 6);
        // the element's aftershock blooms at ground zero (fire ring, gravity
        // well, blizzard lattice, bloom, storm — the soul of the old X)
        if (owner != null) {
            ElementSkills.xAftermath(Element.byId(elementId), level, position(), owner);
        }
        level.playSound(null, getX(), getY(), getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 3.0F, 0.6F);
        level.playSound(null, getX(), getY(), getZ(),
                SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 3.0F, 0.5F);
        level.playSound(null, getX(), getY(), getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.0F, 0.5F);
        // everyone close enough FEELS the hit — hard camera shake at ground zero
        net.neoforged.neoforge.network.PacketDistributor.sendToAllPlayers(
                new ImpactShakePacket(getX(), getY(), getZ(), 1.4F));
        discard();
    }

    // ---------------- spawn data ----------------
    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        buffer.writeUUID(ownerId == null ? new UUID(0L, 0L) : ownerId);
        buffer.writeInt(elementId);
        buffer.writeInt(states.size());
        for (BlockState state : states) {
            buffer.writeInt(Block.BLOCK_STATE_REGISTRY.getId(state));
        }
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buffer) {
        ownerId = buffer.readUUID();
        elementId = buffer.readInt();
        int count = buffer.readInt();
        states.clear();
        for (int i = 0; i < count; i++) {
            states.add(Block.stateById(buffer.readInt()));
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
