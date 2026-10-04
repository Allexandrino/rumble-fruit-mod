package com.rumblefruit;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import com.rumblefruit.earth.EarthBorderTracker;
import com.rumblefruit.earth.EarthData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

// /rumblefruit earth — древний мир; debug-команды для карт/дорог/структур
@EventBusSubscriber(modid = RumbleFruitMod.MOD_ID)
public class ModCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("rumblefruit")
                // /rumblefruit earth — step into the ancient world (spawn: Rome);
                // /rumblefruit earth <place> — teleport to a city or landmark
                .then(Commands.literal("earth")
                        .executes(ctx -> earthTeleport(ctx.getSource().getPlayerOrException(), "rome"))
                        .then(Commands.argument("place",
                                        com.mojang.brigadier.arguments.StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    for (EarthData.Place p : EarthData.places()) {
                                        builder.suggest(p.id());
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> earthTeleport(ctx.getSource().getPlayerOrException(),
                                        com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "place")))))
                // /rumblefruit citydebug <x> <z> — отладка городского слоя
                .then(Commands.literal("citydebug")
                        .then(Commands.argument("x", com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                                .then(Commands.argument("z", com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                                        .executes(ctx -> {
                                            int x = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "x");
                                            int z = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "z");
                                            String info = com.rumblefruit.earth.EarthCities.debugKind(x, z)
                                                    + " h=" + EarthData.worldHeight(x, z);
                                            ctx.getSource().sendSystemMessage(Component.literal(info));
                                            return 1;
                                        }))))
                // /rumblefruit roaddebug <x> <z> — ближайшая дорога к точке
                .then(Commands.literal("roaddebug")
                        .then(Commands.argument("x", com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                                .then(Commands.argument("z", com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                                        .executes(ctx -> {
                                            int x = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "x");
                                            int z = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "z");
                                            ctx.getSource().sendSystemMessage(Component.literal(
                                                    com.rumblefruit.earth.EarthRoads.debugInfo(x, z)));
                                            return 1;
                                        }))))
                // /rumblefruit structdebug <x> <z> — структуры на колонне
                .then(Commands.literal("structdebug")
                        .then(Commands.argument("x", com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                                .then(Commands.argument("z", com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                                        .executes(ctx -> {
                                            int x = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "x");
                                            int z = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "z");
                                            ctx.getSource().sendSystemMessage(Component.literal(
                                                    com.rumblefruit.earth.EarthStructures.debugAt(x, z)));
                                            return 1;
                                        }))))
                // /rumblefruit roadlog — какие пары городов построились/скипнулись
                .then(Commands.literal("roadlog")
                        .executes(ctx -> {
                            for (String s : com.rumblefruit.earth.EarthRoads.roadLog()) {
                                ctx.getSource().sendSystemMessage(Component.literal(s));
                            }
                            return 1;
                        }))
                // /rumblefruit villagedebug — деревни вдоль дорог
                .then(Commands.literal("villagedebug")
                        .executes(ctx -> {
                            var spots = com.rumblefruit.earth.EarthRoads.VILLAGE_SPOTS;
                            ctx.getSource().sendSystemMessage(Component.literal("villages=" + spots.size()));
                            for (int i = 0; i < Math.min(12, spots.size()); i++) {
                                int[] s = spots.get(i);
                                ctx.getSource().sendSystemMessage(Component.literal(s[0] + " " + s[1]));
                            }
                            return 1;
                        })));
    }

    private static int earthTeleport(ServerPlayer player, String placeId) {
        EarthData.Place place = EarthData.findPlace(placeId);
        if (place == null) {
            player.displayClientMessage(Component.translatable("rumblefruit.earth_unknown_place")
                    .withStyle(net.minecraft.ChatFormatting.RED), false);
            return 0;
        }
        ServerLevel earth = player.server.getLevel(EarthBorderTracker.EARTH);
        if (earth == null) {
            player.displayClientMessage(Component.translatable("rumblefruit.earth_missing")
                    .withStyle(net.minecraft.ChatFormatting.RED), false);
            return 0;
        }
        int x = EarthData.blockFromLon(place.lon());
        int z = EarthData.blockFromLat(place.lat());
        int y = EarthData.worldHeight(x, z) + 2;
        player.teleportTo(earth, x + 0.5, Math.max(y, EarthData.SEA_LEVEL + 2), z + 0.5,
                0.0F, 0.0F);
        player.displayClientMessage(Component.translatable("rumblefruit.earth_arrived",
                        Component.literal(place.nameRu()).withStyle(net.minecraft.ChatFormatting.YELLOW))
                .withStyle(net.minecraft.ChatFormatting.GREEN), false);
        return 1;
    }
}
