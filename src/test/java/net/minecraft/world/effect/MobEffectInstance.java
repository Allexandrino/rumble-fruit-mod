package net.minecraft.world.effect;

import net.minecraft.core.Holder;

// vacuum fake of minecraft's MobEffectInstance (effects are Holder-wrapped in 1.21)
public class MobEffectInstance {
    public static final int INFINITE_DURATION = -1;
    public final Holder<MobEffect> effect;
    public final int duration;
    public final int amplifier;

    public MobEffectInstance(Holder<MobEffect> effect, int duration, int amplifier) {
        this(effect, duration, amplifier, false, false);
    }

    public MobEffectInstance(Holder<MobEffect> effect, int duration, int amplifier,
                             boolean ambient, boolean visible) {
        this.effect = effect;
        this.duration = duration;
        this.amplifier = amplifier;
    }
}
