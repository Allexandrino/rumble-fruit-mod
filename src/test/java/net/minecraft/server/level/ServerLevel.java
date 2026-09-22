package net.minecraft.server.level;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

// vacuum fake of minecraft's ServerLevel — records particles, sounds, explosions
public class ServerLevel extends Level {
    public static class ParticleCall {
        public final Object type;
        public final double x;
        public final double y;
        public final double z;
        public final int count;
        public final double dx;
        public final double dy;
        public final double dz;
        public final double speed;
        public final boolean longDistance;

        public ParticleCall(Object type, double x, double y, double z, int count,
                            double dx, double dy, double dz, double speed) {
            this(type, false, x, y, z, count, dx, dy, dz, speed);
        }

        public ParticleCall(Object type, boolean longDistance, double x, double y, double z, int count,
                            double dx, double dy, double dz, double speed) {
            this.type = type;
            this.longDistance = longDistance;
            this.x = x;
            this.y = y;
            this.z = z;
            this.count = count;
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
            this.speed = speed;
        }
    }

    public final List<ParticleCall> particles = new ArrayList<>();
    private final List<ServerPlayer> players = new ArrayList<>();

    // real ServerLevel keeps the online players here — particles flagged
    // long-distance are sent to every one of them (512 block visibility)
    public List<ServerPlayer> players() {
        return players;
    }
    public final List<BlastCall> explosions = new ArrayList<>();
    public final List<Entity> freshEntities = new ArrayList<>();
    private List<? extends Entity> queryResult = List.of();
    private BlockHitResult clipResult = BlockHitResult.miss(Vec3.ZERO);

    public static class BlastCall {
        public final double x;
        public final double y;
        public final double z;
        public final float radius;

        public BlastCall(double x, double y, double z, float radius) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
        }
    }

    public void setQueryResult(List<? extends Entity> entities) {
        this.queryResult = entities;
    }

    public void setClipResult(BlockHitResult result) {
        this.clipResult = result;
    }

    public <T extends net.minecraft.core.particles.ParticleOptions> int sendParticles(
            T type, double x, double y, double z, int count,
            double dx, double dy, double dz, double speed) {
        particles.add(new ParticleCall(type, x, y, z, count, dx, dy, dz, speed));
        return count;
    }

    public <T extends net.minecraft.core.particles.ParticleOptions> int sendParticles(
            T type, boolean longDistance, double x, double y, double z, int count,
            double dx, double dy, double dz, double speed) {
        particles.add(new ParticleCall(type, longDistance, x, y, z, count, dx, dy, dz, speed));
        return count;
    }

    public <T extends net.minecraft.core.particles.ParticleOptions> boolean sendParticles(
            ServerPlayer player, T type, boolean longDistance, double x, double y, double z, int count,
            double dx, double dy, double dz, double speed) {
        particles.add(new ParticleCall(type, longDistance, x, y, z, count, dx, dy, dz, speed));
        return true;
    }

    public <T extends Entity> List<T> getEntitiesOfClass(Class<T> cls, AABB box, Predicate<? super T> predicate) {
        List<T> out = new ArrayList<>();
        for (Entity e : queryResult) {
            if (cls.isInstance(e) && predicate.test(cls.cast(e))) {
                out.add(cls.cast(e));
            }
        }
        return out;
    }

    public boolean addFreshEntity(Entity entity) {
        return freshEntities.add(entity);
    }

    // fake terrain: everything is solid stone unless carved away or built over
    private final java.util.Set<net.minecraft.core.BlockPos> carvedAir = new java.util.HashSet<>();
    private final java.util.Map<net.minecraft.core.BlockPos, net.minecraft.world.level.block.state.BlockState> placed =
            new java.util.HashMap<>();
    private net.minecraft.world.level.block.Block blockAt = net.minecraft.world.level.block.Blocks.STONE;

    public net.minecraft.world.level.block.state.BlockState getBlockState(net.minecraft.core.BlockPos pos) {
        if (carvedAir.contains(pos)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return placed.getOrDefault(pos, blockAt.defaultBlockState());
    }

    public boolean setBlock(net.minecraft.core.BlockPos pos,
                            net.minecraft.world.level.block.state.BlockState state, int flags) {
        if (state.isAir()) {
            carvedAir.add(pos);
            placed.remove(pos);
        } else {
            placed.put(pos, state);
            carvedAir.remove(pos);
        }
        return true;
    }

    public void setDefaultBlock(net.minecraft.world.level.block.Block block) {
        this.blockAt = block;
    }

    public BlockHitResult clip(ClipContext context) {
        return clipResult;
    }

    public net.minecraft.core.BlockPos getSharedSpawnPos() {
        return net.minecraft.core.BlockPos.ZERO;
    }

    @Override
    public net.minecraft.world.level.Explosion explode(Entity exploder, double x, double y, double z,
                                                       float radius, ExplosionInteraction interaction) {
        explosions.add(new BlastCall(x, y, z, radius));
        return super.explode(exploder, x, y, z, radius, interaction);
    }
}
