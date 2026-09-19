package com.rumblefruit;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;

// vacuum fake: shadows the real ModParticles (its DeferredRegister needs a live
// game). the field type matches exactly so compiled callers never notice
public class ModParticles {
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ELECTRO_SLASH =
            DeferredHolder.create(ResourceLocation.fromNamespaceAndPath("minecraft", "particle_type"),
                    ResourceLocation.fromNamespaceAndPath("rumblefruit", "electro_slash"));
}
