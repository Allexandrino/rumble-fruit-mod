package net.minecraft.world.effect;

import net.minecraft.core.Holder;

// vacuum fake of minecraft's MobEffects constants (Holder-wrapped like 1.21)
public class MobEffects {
    public static final Holder<MobEffect> DAMAGE_RESISTANCE = Holder.direct(new MobEffect());
    public static final Holder<MobEffect> GLOWING = Holder.direct(new MobEffect());
    public static final Holder<MobEffect> SLOW_FALLING = Holder.direct(new MobEffect());
    public static final Holder<MobEffect> BLINDNESS = Holder.direct(new MobEffect());
    public static final Holder<MobEffect> MOVEMENT_SLOWDOWN = Holder.direct(new MobEffect());
    public static final Holder<MobEffect> WITHER = Holder.direct(new MobEffect());
    public static final Holder<MobEffect> POISON = Holder.direct(new MobEffect());
}
