package com.rumblefruit;

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

    public static final DeferredHolder<Item, ?> FALLEN_EXORCIST_SPAWN_EGG =
            ITEMS.register("fallen_exorcist_spawn_egg",
                    () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.FALLEN_EXORCIST,
                            0x1a0a10, 0xFFD24A, new Item.Properties()));

    public static final DeferredHolder<Item, ?> BOSS_MAP =
            ITEMS.register("boss_map", BossMapItem::new);

    public static final DeferredHolder<Item, ?> METEOR_CORE_ITEM =
            ITEMS.register("meteor_core", () -> new net.minecraft.world.item.BlockItem(
                    ModBlocks.METEOR_CORE.get(), new Item.Properties()));

    public RumbleFruitMod(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        ModParticles.PARTICLES.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
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
                        output.accept(FALLEN_EXORCIST_SPAWN_EGG.get());
                        output.accept(BOSS_MAP.get());
                        output.accept(METEOR_CORE_ITEM.get());
                    })
                    .build());
}
