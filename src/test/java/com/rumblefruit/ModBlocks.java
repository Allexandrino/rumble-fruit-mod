package com.rumblefruit;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

// vacuum fake of ModBlocks — the deferred registry needs a live game
public class ModBlocks {
    public static final net.neoforged.neoforge.registries.DeferredHolder<Block, ?> BOSS_PEDESTAL =
            new net.neoforged.neoforge.registries.DeferredHolder<>(Blocks.COMMAND_BLOCK);
    public static final net.neoforged.neoforge.registries.DeferredHolder<Block, ?> METEOR_CORE =
            new net.neoforged.neoforge.registries.DeferredHolder<>(Blocks.GLOWSTONE);
}
