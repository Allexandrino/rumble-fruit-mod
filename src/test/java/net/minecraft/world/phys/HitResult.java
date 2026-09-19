package net.minecraft.world.phys;

// vacuum fake of minecraft's HitResult
public class HitResult {
    public enum Type {
        MISS, BLOCK, ENTITY
    }

    private final Type type;
    private final Vec3 location;

    protected HitResult(Vec3 location, Type type) {
        this.location = location;
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public Vec3 getLocation() {
        return location;
    }
}
