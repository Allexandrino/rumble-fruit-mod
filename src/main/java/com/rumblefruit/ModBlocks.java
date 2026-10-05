package com.rumblefruit;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

// блоки древнего мира: травертин (стены Рима), мрамор (храмы),
// мозаичная плитка (полы), римская черепица (крыши)
public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(net.minecraft.core.registries.BuiltInRegistries.BLOCK, RumbleFruitMod.MOD_ID);

    public static final DeferredHolder<Block, ?> TRAVERTINE =
            BLOCKS.register("travertine", () -> new Block(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS)));
    public static final DeferredHolder<Block, ?> MARBLE =
            BLOCKS.register("marble", () -> new Block(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(Blocks.QUARTZ_BLOCK)));
    public static final DeferredHolder<Block, ?> MOSAIC_TILE =
            BLOCKS.register("mosaic_tile", () -> new Block(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)));
    public static final DeferredHolder<Block, ?> ROMAN_ROOF_TILE =
            BLOCKS.register("roman_roof_tile", () -> new Block(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(Blocks.BRICKS)));
}
