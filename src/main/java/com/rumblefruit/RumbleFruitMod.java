package com.rumblefruit;

import com.rumblefruit.earth.EarthBiomeSource;
import com.rumblefruit.earth.EarthChunkGenerator;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

@Mod(RumbleFruitMod.MOD_ID)
public class RumbleFruitMod {
    public static final String MOD_ID = "rumblefruit";

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(net.minecraft.core.registries.BuiltInRegistries.ITEM, MOD_ID);

    public static final DeferredHolder<Item, ?> ELECTRO_APPLE =
            ITEMS.register("electro_apple", ElectroAppleItem::new);

    public static final DeferredHolder<Item, ?> INFERNO_FRUIT =
            ITEMS.register("inferno_fruit", () -> new ElementFruitItem(1));
    public static final DeferredHolder<Item, ?> VOID_FRUIT =
            ITEMS.register("void_fruit", () -> new ElementFruitItem(2));
    public static final DeferredHolder<Item, ?> FROST_FRUIT =
            ITEMS.register("frost_fruit", () -> new ElementFruitItem(3));
    public static final DeferredHolder<Item, ?> NATURE_FRUIT =
            ITEMS.register("nature_fruit", () -> new ElementFruitItem(4));

    public static final DeferredHolder<Item, ?> ELECTRO_BOW =
            ITEMS.register("electro_bow", ElectroBowItem::new);

    public static final DeferredHolder<Item, ?> ELECTRO_SWORD =
            ITEMS.register("electro_sword", ElectroSwordItem::new);

    // projectile-only item: the Z skill orb renders as this (never in the creative tab)
    public static final DeferredHolder<Item, ?> ELECTRO_ORB_ITEM =
            ITEMS.register("electro_orb", () -> new Item(new Item.Properties()));

    // holy variants of the stance weapons (rendered while the angel form is active)
    public static final DeferredHolder<Item, ?> ELECTRO_SWORD_HOLY =
            ITEMS.register("electro_sword_holy", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, ?> ELECTRO_BOW_HOLY =
            ITEMS.register("electro_bow_holy", () -> new Item(new Item.Properties()));

    // блоки древнего мира
    public static final DeferredHolder<Item, ?> TRAVERTINE_ITEM =
            ITEMS.register("travertine", () -> new net.minecraft.world.item.BlockItem(
                    ModBlocks.TRAVERTINE.get(), new Item.Properties()));
    public static final DeferredHolder<Item, ?> MARBLE_ITEM =
            ITEMS.register("marble", () -> new net.minecraft.world.item.BlockItem(
                    ModBlocks.MARBLE.get(), new Item.Properties()));
    public static final DeferredHolder<Item, ?> MOSAIC_TILE_ITEM =
            ITEMS.register("mosaic_tile", () -> new net.minecraft.world.item.BlockItem(
                    ModBlocks.MOSAIC_TILE.get(), new Item.Properties()));
    public static final DeferredHolder<Item, ?> ROMAN_ROOF_TILE_ITEM =
            ITEMS.register("roman_roof_tile", () -> new net.minecraft.world.item.BlockItem(
                    ModBlocks.ROMAN_ROOF_TILE.get(), new Item.Properties()));

    public RumbleFruitMod(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        ModParticles.PARTICLES.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
        // the earth dimension: real-heightmap terrain + latitude biome bands
        // (DeferredRegister fires before the vanilla registries freeze —
        //  direct Registry.register in the constructor is already too late)
        CHUNK_GENERATORS.register(modEventBus);
        BIOME_SOURCES.register(modEventBus);
    }

    public static final DeferredRegister<com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.chunk.ChunkGenerator>>
            CHUNK_GENERATORS = DeferredRegister.create(net.minecraft.core.registries.BuiltInRegistries.CHUNK_GENERATOR, MOD_ID);
    public static final DeferredRegister<com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.biome.BiomeSource>>
            BIOME_SOURCES = DeferredRegister.create(net.minecraft.core.registries.BuiltInRegistries.BIOME_SOURCE, MOD_ID);

    static {
        CHUNK_GENERATORS.register("earth", () -> EarthChunkGenerator.CODEC);
        BIOME_SOURCES.register("earth", () -> EarthBiomeSource.CODEC);
    }

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BLOX_FRUITS_TAB =
            CREATIVE_TABS.register("bloxfruits", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.bloxfruits"))
                    .icon(() -> new ItemStack(ELECTRO_APPLE.get()))
                    .displayItems((params, output) -> {
                        output.accept(ELECTRO_APPLE.get());
                        output.accept(INFERNO_FRUIT.get());
                        output.accept(VOID_FRUIT.get());
                        output.accept(FROST_FRUIT.get());
                        output.accept(NATURE_FRUIT.get());
                        output.accept(TRAVERTINE_ITEM.get());
                        output.accept(MARBLE_ITEM.get());
                        output.accept(MOSAIC_TILE_ITEM.get());
                        output.accept(ROMAN_ROOF_TILE_ITEM.get());
                    })
                    .build());
}
