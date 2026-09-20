package net.minecraft.core;

// vacuum fake of minecraft's BlockPos
public class BlockPos {
    public static final BlockPos ZERO = new BlockPos(0, 0, 0);
    private final int x;
    private final int y;
    private final int z;

    public BlockPos(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static BlockPos containing(double x, double y, double z) {
        return new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }

    public static BlockPos containing(Position pos) {
        return containing(pos.x(), pos.y(), pos.z());
    }

    public BlockPos offset(int dx, int dy, int dz) {
        return new BlockPos(x + dx, y + dy, z + dz);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BlockPos p && p.x == x && p.y == y && p.z == z;
    }

    @Override
    public int hashCode() {
        return x * 961 + y * 31 + z;
    }
}
