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

        public ParticleCall(Object type, double x, double y, double z, int count,
                            double dx, double dy, double dz, double speed) {
            this.type = type;
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

    public BlockHitResult clip(ClipContext context) {
        return clipResult;
    }

    @Override
    public net.minecraft.world.level.Explosion explode(Entity exploder, double x, double y, double z,
                                                       float radius, ExplosionInteraction interaction) {
        explosions.add(new BlastCall(x, y, z, radius));
        return super.explode(exploder, x, y, z, radius, interaction);
    }
}
