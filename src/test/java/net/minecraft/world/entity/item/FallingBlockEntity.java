package net.minecraft.world.entity.item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

// vacuum fake of minecraft's FallingBlockEntity — a block entity that falls
public class FallingBlockEntity extends net.minecraft.world.entity.Entity {
    public final BlockState blockState;

    public FallingBlockEntity(Level level, double x, double y, double z, BlockState state) {
        setLevel(level);
        setPos(x, y, z);
        setOnGround(false);
        this.blockState = state;
    }

    public static FallingBlockEntity fall(Level level, BlockPos pos, BlockState state) {
        FallingBlockEntity entity = new FallingBlockEntity(level,
                pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, state);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.addFreshEntity(entity);
        }
        return entity;
    }
}
