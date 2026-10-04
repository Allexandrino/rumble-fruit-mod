package com.rumblefruit;

import com.rumblefruit.earth.EarthBiomeSource;
import com.rumblefruit.earth.EarthChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;

// ancient-earth mod: the real-heightmap Mediterranean at 1 block = 1 meter,
// ancient cities with unique landmarks, roman roads, minimap on M
@Mod(RumbleFruitMod.MOD_ID)
public class RumbleFruitMod {
    public static final String MOD_ID = "rumblefruit";

    public RumbleFruitMod(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
        // the earth dimension: real-heightmap terrain + latitude biome bands
        // (DeferredRegister fires before the vanilla registries freeze —
        //  direct Registry.register in the constructor is already too late)
        CHUNK_GENERATORS.register(modEventBus);
        BIOME_SOURCES.register(modEventBus);
    }

    public static final DeferredRegister<net.minecraft.world.item.Item> ITEMS =
            DeferredRegister.create(net.minecraft.core.registries.BuiltInRegistries.ITEM, MOD_ID);

    // стихийные фрукты: еда героя — даёт сопротивление и лишние сердца
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.item.Item, ?>
            ELECTRO_APPLE = ITEMS.register("electro_apple", ElectroAppleItem::new);
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.item.Item, ?>
            INFERNO_FRUIT = ITEMS.register("inferno_fruit", () -> new ElementFruitItem(1));
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.item.Item, ?>
            VOID_FRUIT = ITEMS.register("void_fruit", () -> new ElementFruitItem(2));
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.item.Item, ?>
            FROST_FRUIT = ITEMS.register("frost_fruit", () -> new ElementFruitItem(3));
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.item.Item, ?>
            NATURE_FRUIT = ITEMS.register("nature_fruit", () -> new ElementFruitItem(4));

    public static final DeferredRegister<net.minecraft.world.item.CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.item.CreativeModeTab, net.minecraft.world.item.CreativeModeTab>
            BLOX_FRUITS_TAB = CREATIVE_TABS.register("bloxfruits", () -> net.minecraft.world.item.CreativeModeTab.builder()
            .title(net.minecraft.network.chat.Component.translatable("itemGroup.bloxfruits"))
            .icon(() -> new net.minecraft.world.item.ItemStack(ELECTRO_APPLE.get()))
            .displayItems((params, output) -> {
                output.accept(ELECTRO_APPLE.get());
                output.accept(INFERNO_FRUIT.get());
                output.accept(VOID_FRUIT.get());
                output.accept(FROST_FRUIT.get());
                output.accept(NATURE_FRUIT.get());
            })
            .build());

    public static final DeferredRegister<com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.chunk.ChunkGenerator>>
            CHUNK_GENERATORS = DeferredRegister.create(net.minecraft.core.registries.BuiltInRegistries.CHUNK_GENERATOR, MOD_ID);
    public static final DeferredRegister<com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.biome.BiomeSource>>
            BIOME_SOURCES = DeferredRegister.create(net.minecraft.core.registries.BuiltInRegistries.BIOME_SOURCE, MOD_ID);

    static {
        CHUNK_GENERATORS.register("earth", () -> EarthChunkGenerator.CODEC);
        BIOME_SOURCES.register("earth", () -> EarthBiomeSource.CODEC);
    }
}
