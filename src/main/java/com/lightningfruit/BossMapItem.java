package com.lightningfruit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.ChatFormatting;

// the Boss Map: sold by cartographer villagers. right click teleports the player to
// the Chambers of the Fallen Exorcist (a floating sky temple). the map is NOT consumed —
// it must be placed on the boss pedestal at the center to start the fight
public class BossMapItem extends Item {

    public BossMapItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)
                || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(stack);
        }
        BlockPos center = BossChamber.getOrCreate(serverLevel);
        // golden flash at the departure point
        serverLevel.sendParticles(ParticleTypes.END_ROD,
                player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.5, 1.0, 0.5, 0.08);
        // teleport to the temple entrance (north end of the red carpet)
        serverPlayer.teleportTo(center.getX() + 0.5, center.getY() + 1.0, center.getZ() - 8.5);
        serverLevel.sendParticles(ParticleTypes.END_ROD,
                center.getX() + 0.5, center.getY() + 1.5, center.getZ() - 8.5, 60, 0.5, 1.0, 0.5, 0.08);
        serverLevel.playSound(null, center, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 2.0F, 1.2F);
        serverLevel.playSound(null, center, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 2.0F, 1.4F);
        player.displayClientMessage(Component.translatable("lightningfruit.chamber_teleport")
                .withStyle(ChatFormatting.GOLD), true);
        return InteractionResultHolder.consume(stack);
    }
}
