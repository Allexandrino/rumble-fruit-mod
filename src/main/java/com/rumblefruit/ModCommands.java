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
                        }))
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
                                        com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "place"))))));
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
        int y = EarthData.surfaceHeight(x, z) + 2;
        player.teleportTo(earth, x + 0.5, Math.max(y, EarthData.SEA_LEVEL + 2), z + 0.5,
                0.0F, 0.0F);
        player.displayClientMessage(Component.translatable("rumblefruit.earth_arrived",
                        Component.literal(place.nameRu()).withStyle(net.minecraft.ChatFormatting.YELLOW))
                .withStyle(net.minecraft.ChatFormatting.GREEN), false);
        return 1;
    }
}
