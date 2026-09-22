package com.rumblefruit;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

// the smouldering heart of a fallen meteor: right-click it and the dungeon
// drags you in — fruit in hand, Cube Titan waiting
public class MeteorCoreBlock extends Block {

    public MeteorCoreBlock() {
        super(Properties.of()
                .strength(50.0F, 1200.0F)
                .lightLevel(state -> 12)
                .sound(SoundType.ANCIENT_DEBRIS)
                .requiresCorrectToolForDrops());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            MeteorDungeon.enter((ServerLevel) level, serverPlayer);
        }
        return InteractionResult.SUCCESS;
    }
}
