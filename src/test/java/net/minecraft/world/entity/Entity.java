package net.minecraft.world.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

// vacuum fake of minecraft's Entity — records motion, knows its position
public class Entity {
    public boolean hurtMarked = false;
    public float fallDistance = 0.0F;

    private double x;
    private double y;
    private double z;
    private Vec3 delta = Vec3.ZERO;
    private float yRot = 0.0F;
    private float xRot = 0.0F;
    private boolean alive = true;
    private boolean onGround = true;
    private final UUID uuid = UUID.randomUUID();
    private Level level = new Level();

    public Vec3 position() {
        return new Vec3(x, y, z);
    }

    public void setPos(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public Vec3 getDeltaMovement() {
        return delta;
    }

    public void setDeltaMovement(Vec3 v) {
        this.delta = v;
    }

    public void setDeltaMovement(double dx, double dy, double dz) {
        this.delta = new Vec3(dx, dy, dz);
    }

    public void push(double dx, double dy, double dz) {
        this.delta = this.delta.add(dx, dy, dz);
    }

    public float getYRot() {
        return yRot;
    }

    public void setYRot(float yRot) {
        this.yRot = yRot;
    }

    public float getXRot() {
        return xRot;
    }

    public void setXRot(float xRot) {
        this.xRot = xRot;
    }

    // same math as the real thing
    public Vec3 getLookAngle() {
        float yaw = -yRot * 0.0174533F - (float) Math.PI;
        float pitch = -xRot * 0.0174533F;
        double cosYaw = Math.cos(yaw);
        double sinYaw = Math.sin(yaw);
        double cosPitch = Math.cos(pitch);
        double sinPitch = Math.sin(pitch);
        return new Vec3(sinYaw * cosPitch, sinPitch, cosYaw * cosPitch);
    }

    public Vec3 getEyePosition() {
        return new Vec3(x, y + 1.62, z);
    }

    public float getBbHeight() {
        return 1.8F;
    }

    public AABB getBoundingBox() {
        return new AABB(x - 0.3, y, z - 0.3, x + 0.3, y + 1.8, z + 0.3);
    }

    public boolean isAlive() {
        return alive;
    }

    public boolean onGround() {
        return onGround;
    }

    public boolean isInWater() {
        return false;
    }

    public Level level() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public UUID getUUID() {
        return uuid;
    }

    public BlockPos blockPosition() {
        return BlockPos.containing(x, y, z);
    }
}
