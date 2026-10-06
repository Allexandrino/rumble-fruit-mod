package com.rumblefruit;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

// команды мода: charge / fruit / titan / terra
@EventBusSubscriber(modid = RumbleFruitMod.MOD_ID)
public class ModCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("rumblefruit")
                // /rumblefruit charge — полный заряд силы
                .then(Commands.literal("charge")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            PowerChargeData.add(player, PowerChargeData.MAX);
                            PowerChargeData.sync(player);
                            return 1;
                        }))
                // /rumblefruit fruit <element> — сила фрукта без поедания (демо/съёмка)
                .then(Commands.literal("fruit")
                        .then(Commands.argument("element",
                                        com.mojang.brigadier.arguments.StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    for (String k : new String[]{"lightning", "inferno", "void", "frost", "nature"}) {
                                        builder.suggest(k);
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String name = com.mojang.brigadier.arguments.StringArgumentType
                                            .getString(ctx, "element");
                                    int elementId = switch (name) {
                                        case "inferno" -> 1;
                                        case "void" -> 2;
                                        case "frost" -> 3;
                                        case "nature" -> 4;
                                        default -> 0;
                                    };
                                    RumblePowerData.grant(player, elementId);
                                    PowerChargeData.add(player, PowerChargeData.MAX);
                                    PowerChargeData.sync(player);
                                    return 1;
                                })))
                // /rumblefruit titan — мгновенная титаническая форма (демо/съёмка)
                .then(Commands.literal("titan")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            if (!RumblePowerData.hasPower(player)) {
                                RumblePowerData.grant(player);
                            }
                            PowerChargeData.add(player, PowerChargeData.MAX);
                            PowerChargeData.sync(player);
                            if (!WingsData.isActive(player.getUUID())) {
                                WingsData.setActive(player, true);
                            }
                            return 1;
                        }))
                // /rumblefruit terra — земляной разлом по прицелу (демо/съёмка)
                .then(Commands.literal("terra")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            if (!RumblePowerData.hasPower(player)) {
                                RumblePowerData.grant(player);
                            }
                            TerraSkills.rip(player, Element.byId(RumblePowerData.elementOf(player)));
                            return 1;
                        })));
    }

}
