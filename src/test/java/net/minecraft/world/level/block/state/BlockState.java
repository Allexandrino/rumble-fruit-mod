package net.minecraft.world.level.block.state;

import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;

// vacuum fake of minecraft's BlockState
public class BlockState {
    private final Block block;

    public BlockState(Block block) {
        this.block = block;
    }

    public Block getBlock() {
        return block;
    }

    public boolean isSolid() {
        return !block.isAir();
    }

    public boolean isAir() {
        return block.isAir();
    }

    // -1 means indestructible (bedrock & friends), like the real game
    public float getDestroySpeed(BlockGetter level, net.minecraft.core.BlockPos pos) {
        return block.isIndestructible() ? -1.0F : 1.5F;
    }
}
