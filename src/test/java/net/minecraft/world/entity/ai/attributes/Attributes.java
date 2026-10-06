package net.minecraft.world.entity.ai.attributes;

import net.minecraft.core.Holder;

// vacuum fake of minecraft's Attributes registry constants (Holder-wrapped in 1.21)
public class Attributes {
    public static final Holder<Attribute> MAX_HEALTH = Holder.direct(new Attribute());
    // titan forms grow through these
    public static final Holder<Attribute> SCALE = Holder.direct(new Attribute());
    public static final Holder<Attribute> STEP_HEIGHT = Holder.direct(new Attribute());
    public static final Holder<Attribute> KNOCKBACK_RESISTANCE = Holder.direct(new Attribute());
    public static final Holder<Attribute> SAFE_FALL_DISTANCE = Holder.direct(new Attribute());
    public static final Holder<Attribute> BLOCK_INTERACTION_RANGE = Holder.direct(new Attribute());
    public static final Holder<Attribute> ENTITY_INTERACTION_RANGE = Holder.direct(new Attribute());
}
