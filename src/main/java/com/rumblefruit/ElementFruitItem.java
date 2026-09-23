package com.rumblefruit;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;

// an elemental devil fruit (inferno / void / frost / nature): eating it
// grants that element's powers, replacing whatever fruit you had
public class ElementFruitItem extends Item {
    private final int element;

    public ElementFruitItem(int element) {
        super(new Properties()
                .food(new FoodProperties.Builder().nutrition(4).saturationModifier(1.2F).build())
                .stacksTo(16)
                .rarity(Rarity.EPIC));
        this.element = element;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, net.minecraft.world.entity.LivingEntity entity) {
        if (entity instanceof Player player) {
            RumblePowerData.grant(player, element);
        }
        return super.finishUsingItem(stack, level, entity);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }
}
