package com.rumblefruit;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

// the Exorcist Realm: a void dimension holding the Fallen Exorcist's
// colossal chambers (built on first entry)
public final class MeteorRealm {

    private MeteorRealm() {
    }

    public static final ResourceKey<Level> KEY = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "exorcist_realm"));
}
