package com.rumblefruit;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// meteors fall on a fixed schedule: every 20 minutes a blazing magma block
// tears down from high above a random player, smashes a small crater and
// leaves a METEOR CORE smouldering in it. right-clicking the core drags you
// into the dungeon. a HUD countdown shows the time to the next fall.
@EventBusSubscriber(modid = RumbleFruitMod.MOD_ID)
public class MeteorShower {

    private MeteorShower() {
    }

    static final long INTERVAL_TICKS = 24000; // exactly 20 minutes
    private static final double SPAWN_HEIGHT = 55.0;
    private static final double SPAWN_SPREAD = 40.0;
    private static final double ENTRY_SPEED = -1.5; // meteors dive fast
    private static final float IMPACT_POWER = 3.0F;

    // package-visible and swappable in tests so the spawn points are deterministic
    static Random RANDOM = new Random();
    private static final Map<UUID, FallingBlockEntity> FALLING = new ConcurrentHashMap<>();
    private static long nextMeteorAt = -1;

    // test hook: drop all tracked meteors and the schedule
    static void clear() {
        FALLING.clear();
        nextMeteorAt = -1;
    }

    // ticks left until the next scheduled fall
    static long remainingTicks(long now) {
        return nextMeteorAt < 0 ? INTERVAL_TICKS : Math.max(0L, nextMeteorAt - now);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel level = event.getServer().overworld();
        tickSchedule(level);
        if (level.getGameTime() % 20 == 0 && !level.players().isEmpty()) {
            net.neoforged.neoforge.network.PacketDistributor.sendToAllPlayers(
                    new MeteorCountdownPacket(remainingTicks(level.getGameTime())));
        }
        tickMeteors();
    }

    // the schedule: every INTERVAL_TICKS a meteor falls above a random player
    static boolean tickSchedule(ServerLevel level) {
        if (level.players().isEmpty()) {
            return false;
        }
        long now = level.getGameTime();
        if (nextMeteorAt < 0) {
            nextMeteorAt = now + INTERVAL_TICKS;
        }
        if (now < nextMeteorAt) {
            return false;
        }
        ServerPlayer lucky = level.players().get(RANDOM.nextInt(level.players().size()));
        spawnMeteor(level, lucky);
        nextMeteorAt = now + INTERVAL_TICKS;
        return true;
    }

    // a meteor tears into the sky above the player: blazing magma block with
    // an entry flash, a roar and a beacon column visible from kilometers away
    static FallingBlockEntity spawnMeteor(ServerLevel level, ServerPlayer player) {
        double x = player.getX() + (RANDOM.nextDouble() - 0.5) * SPAWN_SPREAD * 2.0;
        double z = player.getZ() + (RANDOM.nextDouble() - 0.5) * SPAWN_SPREAD * 2.0;
        double y = player.getY() + SPAWN_HEIGHT;
        BlockPos pos = BlockPos.containing(x, y, z);
        FallingBlockEntity meteor = FallingBlockEntity.fall(level, pos,
                Blocks.MAGMA_BLOCK.defaultBlockState());
        meteor.setDeltaMovement(0.0, ENTRY_SPEED, 0.0);
        FALLING.put(meteor.getUUID(), meteor);
        level.sendParticles(ModParticles.ELECTRO_GLOW.get(), x, y, z, 40, 1.5, 1.5, 1.5, 0.2);
        // sky tear: a glowing column marking the entry point, force-flagged
        FarFx.column(level, ModParticles.ELECTRO_GLOW.get(), x, y - 30.0, z, 60.0, 2.0, 2, 0.5);
        level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.WEATHER, 3.0F, 0.5F);
        return meteor;
    }

    // every tracked meteor burns a trail; on touchdown it detonates and the
    // meteor core is left smouldering in the crater
    static void tickMeteors() {
        for (var it = FALLING.values().iterator(); it.hasNext(); ) {
            FallingBlockEntity meteor = it.next();
            if (meteor.onGround() || !meteor.isAlive()) {
                it.remove();
                impact(meteor);
            } else {
                trail(meteor);
            }
        }
    }

    private static void trail(FallingBlockEntity meteor) {
        ServerLevel level = (ServerLevel) meteor.level();
        level.sendParticles(ModParticles.ELECTRO_SPARK.get(),
                meteor.getX(), meteor.getY() + 0.5, meteor.getZ(), 12, 0.4, 0.4, 0.4, 0.05);
        level.sendParticles(ModParticles.ELECTRO_GLOW.get(),
                meteor.getX(), meteor.getY() + 0.5, meteor.getZ(), 3, 0.3, 0.3, 0.3, 0.02);
    }

    static void impact(FallingBlockEntity meteor) {
        ServerLevel level = (ServerLevel) meteor.level();
        double x = meteor.getX();
        double y = meteor.getY();
        double z = meteor.getZ();
        level.explode(null, x, y, z, IMPACT_POWER, Level.ExplosionInteraction.BLOCK);
        // the heart of the meteor survives: right-click it to enter the dungeon
        level.setBlock(BlockPos.containing(x, y, z),
                ModBlocks.METEOR_CORE.get().defaultBlockState(), 3);
        level.sendParticles(ModParticles.ELECTRO_GLOW.get(), x, y + 1.0, z, 120, 3.0, 2.0, 3.0, 0.3);
        level.sendParticles(ModParticles.ELECTRO_SPARK.get(), x, y + 1.0, z, 80, 2.0, 1.5, 2.0, 0.2);
        // the impact flash stabs the sky — visible from kilometers away
        FarFx.column(level, ModParticles.ELECTRO_GLOW.get(), x, y, z, 120.0, 2.0, 3, 0.6);
        level.playSound(null, x, y, z, ModSounds.ELECTRO_BLAST.get(), SoundSource.WEATHER, 6.0F, 0.6F);
    }
}
