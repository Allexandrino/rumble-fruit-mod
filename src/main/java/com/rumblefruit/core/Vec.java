package com.rumblefruit.core;

// minimal 3d vector for core math — the vacuum replacement for minecraft's Vec3
public record Vec(double x, double y, double z) {
    public Vec add(Vec o) {
        return new Vec(x + o.x, y + o.y, z + o.z);
    }

    public Vec add(double ox, double oy, double oz) {
        return new Vec(x + ox, y + oy, z + oz);
    }

    public Vec subtract(Vec o) {
        return new Vec(x - o.x, y - o.y, z - o.z);
    }

    public Vec scale(double s) {
        return new Vec(x * s, y * s, z * s);
    }

    public double dot(Vec o) {
        return x * o.x + y * o.y + z * o.z;
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public double distanceTo(Vec o) {
        return subtract(o).length();
    }

    public Vec normalize() {
        double len = length();
        return len == 0.0 ? new Vec(0.0, 0.0, 0.0) : scale(1.0 / len);
    }

    public Vec lerp(Vec o, double k) {
        return new Vec(x + (o.x - x) * k, y + (o.y - y) * k, z + (o.z - z) * k);
    }
}
