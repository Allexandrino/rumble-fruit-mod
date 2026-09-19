package com.lightningfruit;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;

// the Lightning fruit as an edible electro-apple; eating it unlocks the Lightning powers
public class ElectroAppleItem extends Item {
    public ElectroAppleItem() {
        super(new Properties()
                .food(new FoodProperties.Builder().nutrition(4).saturationModifier(1.2F).build())
                .stacksTo(16)
                .rarity(Rarity.EPIC));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, net.minecraft.world.entity.LivingEntity entity) {
        if (entity instanceof Player player) {
            LightningPowerData.grant(player);
        }
        return super.finishUsingItem(stack, level, entity);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // allow eating even when full so players can unlock powers
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }
}
