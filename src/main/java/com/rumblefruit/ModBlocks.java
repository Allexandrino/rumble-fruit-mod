package com.rumblefruit;

import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(net.minecraft.core.registries.BuiltInRegistries.BLOCK, RumbleFruitMod.MOD_ID);

    public static final DeferredHolder<Block, ?> BOSS_PEDESTAL =
            BLOCKS.register("boss_pedestal", BossPedestalBlock::new);

    public static final DeferredHolder<Block, ?> METEOR_CORE =
            BLOCKS.register("meteor_core", MeteorCoreBlock::new);
}
