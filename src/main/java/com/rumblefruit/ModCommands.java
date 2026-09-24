package com.rumblefruit;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

// /rumblefruit meteor — call a meteor down on your position right now
@EventBusSubscriber(modid = RumbleFruitMod.MOD_ID)
public class ModCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("rumblefruit")
                .then(Commands.literal("meteor")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            MeteorShower.spawnMeteor((ServerLevel) player.level(), player);
                            return 1;
                        }))
                .then(Commands.literal("dungeon")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            MeteorDungeon.enter((ServerLevel) player.level(), player);
                            return 1;
                        }))
                .then(Commands.literal("charge")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            PowerChargeData.add(player, PowerChargeData.MAX);
                            PowerChargeData.sync(player);
                            return 1;
                        })));
    }
}
