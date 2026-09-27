package com.rumblefruit;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

// guardian crystals burn on the cavern walls and shield the Fallen Exorcist:
// while any crystal stands, the throne takes no damage. break them all.
@EventBusSubscriber(modid = RumbleFruitMod.MOD_ID)
public class GuardianCrystals {
    private static int alive = 0;

    private GuardianCrystals() {
    }

    public static void arm(int count) {
        alive = count;
    }

    public static int alive() {
        return alive;
    }

    // test hook
    static void reset() {
        alive = 0;
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getState().getBlock() == ModBlocks.GUARDIAN_CRYSTAL.get() && alive > 0) {
            alive--;
            if (event.getPlayer() != null) {
                event.getPlayer().displayClientMessage(net.minecraft.network.chat.Component.translatable(
                                "rumblefruit.crystal_down", alive)
                        .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE), true);
            }
        }
    }
}
