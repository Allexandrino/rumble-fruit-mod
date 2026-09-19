package com.rumblefruit;

import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.event.village.WandererTradesEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

// the boss map is sold by cartographers (and the wandering trader)
@EventBusSubscriber(modid = RumbleFruitMod.MOD_ID)
public class ModVillagerTrades {

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() == VillagerProfession.CARTOGRAPHER) {
            // journeyman trade: 14 emeralds -> boss map
            event.getTrades().get(2).add((trader, random) -> new MerchantOffer(
                    new net.minecraft.world.item.trading.ItemCost(Items.EMERALD, 14),
                    new ItemStack(RumbleFruitMod.BOSS_MAP.get()),
                    6, 10, 0.05F));
        }
    }

    @SubscribeEvent
    public static void onWandererTrades(WandererTradesEvent event) {
        event.getGenericTrades().add((trader, random) -> new MerchantOffer(
                new net.minecraft.world.item.trading.ItemCost(Items.EMERALD, 10),
                new ItemStack(RumbleFruitMod.BOSS_MAP.get()),
                3, 10, 0.05F));
    }
}
