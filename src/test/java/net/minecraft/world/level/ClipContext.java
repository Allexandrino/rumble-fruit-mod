package net.minecraft.world.level;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

// vacuum fake of minecraft's ClipContext (only the shape our code builds)
public class ClipContext {
    public enum Block {
        VISUAL, COLLIDER, OUTLINE
    }

    public enum Fluid {
        NONE, ANY
    }

    public final Vec3 from;
    public final Vec3 to;

    public ClipContext(Vec3 from, Vec3 to, Block block, Fluid fluid, Entity entity) {
        this.from = from;
        this.to = to;
    }
}
