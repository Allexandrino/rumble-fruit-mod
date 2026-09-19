package net.neoforged.neoforge.registries;

import net.minecraft.resources.ResourceLocation;

// vacuum fake of neoforge's DeferredHolder
public class DeferredHolder<R, T> {
    private final T value;

    private DeferredHolder(T value) {
        this.value = value;
    }

    public static <R, T extends R> DeferredHolder<R, T> create(ResourceLocation registryName,
                                                               ResourceLocation valueName) {
        return new DeferredHolder<>(null);
    }

    public T get() {
        return value;
    }
}
