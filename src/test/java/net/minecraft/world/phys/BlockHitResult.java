package net.minecraft.world.phys;

// vacuum fake of minecraft's BlockHitResult
public class BlockHitResult extends HitResult {
    public BlockHitResult(Vec3 location, Type type) {
        super(location, type);
    }

    public static BlockHitResult miss(Vec3 location) {
        return new BlockHitResult(location, Type.MISS);
    }
}
