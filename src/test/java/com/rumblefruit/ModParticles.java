package com.rumblefruit;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.neoforge.registries.DeferredHolder;

// vacuum fake: shadows the real ModParticles (its DeferredRegister needs a live
// game). returns real particle instances so the TYPE is testable
public class ModParticles {
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ELECTRO_SLASH =
            new DeferredHolder<>(new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ELECTRO_SPARK =
            new DeferredHolder<>(new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ELECTRO_GLOW =
            new DeferredHolder<>(new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ELECTRO_CLOUD =
            new DeferredHolder<>(new SimpleParticleType(false));
}
