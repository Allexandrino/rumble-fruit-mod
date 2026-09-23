package com.rumblefruit;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LeavesBlock;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

import java.util.Random;

// electro-apples drop from tree leaves, like apples from oaks
@EventBusSubscriber(modid = RumbleFruitMod.MOD_ID)
public class LeafDrops {
    private static final Random RANDOM = new Random();
    private static final float DROP_CHANCE = 0.02F; // 2% per leaf, like oak apples
    private static final float ELEMENT_CHANCE = 0.005F; // 0.5% per leaf per elemental fruit

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getState().getBlock() instanceof LeavesBlock)) {
            return;
        }
        var pos = event.getPos();
        if (RANDOM.nextFloat() < DROP_CHANCE) {
            drop(event, pos, new ItemStack(RumbleFruitMod.ELECTRO_APPLE.get()));
        }
        // elemental fruits are rarer: inferno, void, frost, nature
        var fruits = java.util.List.of(
                RumbleFruitMod.INFERNO_FRUIT, RumbleFruitMod.VOID_FRUIT,
                RumbleFruitMod.FROST_FRUIT, RumbleFruitMod.NATURE_FRUIT);
        for (var fruit : fruits) {
            if (RANDOM.nextFloat() < ELEMENT_CHANCE) {
                drop(event, pos, new ItemStack(fruit.get()));
            }
        }
    }

    private static void drop(BlockEvent.BreakEvent event, net.minecraft.core.BlockPos pos, ItemStack stack) {
        var item = new ItemEntity((net.minecraft.world.level.Level) event.getLevel(),
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
        event.getLevel().addFreshEntity(item);
    }
}
