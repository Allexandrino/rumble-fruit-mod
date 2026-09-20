package com.rumblefruit;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;

// vacuum fake: shadows the real ModParticles (its DeferredRegister needs a live
// game). returns a real particle instance so the TYPE is testable
public class ModParticles {
    private static final SimpleParticleType SLASH = new SimpleParticleType(false);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ELECTRO_SLASH =
            new DeferredHolder<>(SLASH);
}
