package net.minecraft.world.phys;

// vacuum fake of minecraft's Vec3 — same members our code uses, same signatures
public class Vec3 {
    public static final Vec3 ZERO = new Vec3(0.0, 0.0, 0.0);
    public final double x;
    public final double y;
    public final double z;

    public Vec3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vec3 add(Vec3 o) {
        return new Vec3(x + o.x, y + o.y, z + o.z);
    }

    public Vec3 add(double ox, double oy, double oz) {
        return new Vec3(x + ox, y + oy, z + oz);
    }

    public Vec3 subtract(Vec3 o) {
        return new Vec3(x - o.x, y - o.y, z - o.z);
    }

    public Vec3 scale(double s) {
        return new Vec3(x * s, y * s, z * s);
    }

    public Vec3 multiply(Vec3 o) {
        return new Vec3(x * o.x, y * o.y, z * o.z);
    }

    public double dot(Vec3 o) {
        return x * o.x + y * o.y + z * o.z;
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public double distanceTo(Vec3 o) {
        return subtract(o).length();
    }

    public Vec3 normalize() {
        double len = length();
        return len < 1.0E-9 ? ZERO : scale(1.0 / len);
    }

    public Vec3 lerp(Vec3 to, double k) {
        return new Vec3(x + (to.x - x) * k, y + (to.y - y) * k, z + (to.z - z) * k);
    }
}
