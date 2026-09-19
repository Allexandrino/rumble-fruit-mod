package com.lightningfruit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.ChatFormatting;

// the boss pedestal in the center of the Fallen Exorcist's chambers.
// place the boss map on it (right click) to summon the boss with a storm of golden lightning
public class BossPedestalBlock extends Block {

    public BossPedestalBlock() {
        super(Properties.of().strength(50.0F, 1200.0F).lightLevel(s -> 13).noOcclusion());
    }

    @Override
    public net.minecraft.world.ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level,
                                                               BlockPos pos, Player player,
                                                               InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return net.minecraft.world.ItemInteractionResult.SUCCESS;
        }
        if (!held.is(LightningFruitMod.BOSS_MAP.get())) {
            player.displayClientMessage(Component.translatable("lightningfruit.need_map")
                    .withStyle(ChatFormatting.GRAY), true);
            return net.minecraft.world.ItemInteractionResult.CONSUME;
        }
        // only one living boss at a time
        if (!serverLevel.getEntitiesOfClass(FallenExorcistEntity.class, new AABB(pos).inflate(200.0)).isEmpty()) {
            player.displayClientMessage(Component.translatable("lightningfruit.boss_alive")
                    .withStyle(ChatFormatting.DARK_RED), true);
            return net.minecraft.world.ItemInteractionResult.CONSUME;
        }

        // summon the fallen exorcist above the pedestal
        FallenExorcistEntity boss = new FallenExorcistEntity(ModEntities.FALLEN_EXORCIST.get(), serverLevel);
        boss.setPos(pos.getX() + 0.5, pos.getY() + 3.0, pos.getZ() + 0.5);
        serverLevel.addFreshEntity(boss);
        boss.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, false));

        // epic arrival: a ring of golden lightning + holy particles + thunder
        for (int i = 0; i < 5; i++) {
            double a = i * Math.PI * 2.0 / 5.0;
            ElectroBoltEntity.strike(serverLevel,
                    pos.getX() + 0.5 + Math.cos(a) * 6.0, pos.getY() + 1.0,
                    pos.getZ() + 0.5 + Math.sin(a) * 6.0, boss, true);
        }
        serverLevel.sendParticles(ParticleTypes.END_ROD,
                pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 80, 1.5, 1.5, 1.5, 0.1);
        serverLevel.sendParticles(ParticleTypes.FLASH,
                pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5, 1, 0.0, 0.0, 0.0, 0.0);
        serverLevel.playSound(null, pos, SoundEvents.TOTEM_USE, SoundSource.BLOCKS, 1.0F, 0.8F);
        serverLevel.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 2.0F, 0.8F);
        serverLevel.playSound(null, pos, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 2.0F, 0.6F);

        if (!player.isCreative()) {
            held.shrink(1);
        }
        return net.minecraft.world.ItemInteractionResult.CONSUME;
    }
}
