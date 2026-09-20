package net.minecraft.world.level.block;

import net.minecraft.world.level.block.state.BlockState;

// vacuum fake of minecraft's Block
public class Block {
    public static final int UPDATE_ALL = 3;
    public static final int UPDATE_NONE = 4;

    private final boolean indestructible;
    private final boolean air;

    public Block(boolean indestructible, boolean air) {
        this.indestructible = indestructible;
        this.air = air;
    }

    public BlockState defaultBlockState() {
        return new BlockState(this);
    }

    public boolean isIndestructible() {
        return indestructible;
    }

    public boolean isAir() {
        return air;
    }
}
