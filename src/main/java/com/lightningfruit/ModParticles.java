package com.lightningfruit;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(net.minecraft.core.registries.BuiltInRegistries.PARTICLE_TYPE, LightningFruitMod.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ELECTRO_SLASH =
            PARTICLES.register("electro_slash", () -> new SimpleParticleType(false));
}
