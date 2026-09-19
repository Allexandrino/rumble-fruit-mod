package com.lightningfruit;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LeavesBlock;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

import java.util.Random;

// electro-apples drop from tree leaves, like apples from oaks
@EventBusSubscriber(modid = LightningFruitMod.MOD_ID)
public class LeafDrops {
    private static final Random RANDOM = new Random();
    private static final float DROP_CHANCE = 0.02F; // 2% per leaf, like oak apples

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getState().getBlock() instanceof LeavesBlock)) {
            return;
        }
        if (RANDOM.nextFloat() >= DROP_CHANCE) {
            return;
        }
        var pos = event.getPos();
        var item = new ItemEntity((net.minecraft.world.level.Level) event.getLevel(),
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                new ItemStack(LightningFruitMod.ELECTRO_APPLE.get()));
        event.getLevel().addFreshEntity(item);
    }
}
