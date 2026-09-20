package com.rumblefruit;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(net.minecraft.core.registries.BuiltInRegistries.PARTICLE_TYPE, RumbleFruitMod.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ELECTRO_SLASH =
            PARTICLES.register("electro_slash", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ELECTRO_SPARK =
            PARTICLES.register("electro_spark", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ELECTRO_GLOW =
            PARTICLES.register("electro_glow", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ELECTRO_CLOUD =
            PARTICLES.register("electro_cloud", () -> new SimpleParticleType(false));
}
