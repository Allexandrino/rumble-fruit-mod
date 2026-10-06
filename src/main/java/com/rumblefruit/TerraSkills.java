package com.rumblefruit;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

// terra rip (the X skill for every fruit): the caster tears a 3x3x3 slab out
// of the world itself — БАМ, a smoking crater opens, real fissures crawl
// outward, the slab rises, hangs over the caster's head and hurls at the
// point he's looking at. the slam detonates in debris, cracks and a camera
// shake everyone nearby feels — then the element's aftershock blooms at
// ground zero. cooldown lives in SkillExecutor's X slot.
public final class TerraSkills {

    private TerraSkills() {
    }

    public static void rip(ServerPlayer player, Element element) {
        ServerLevel level = (ServerLevel) player.level();

        // where is he pointing? find real ground under the mark
        Vec3 target = SkillExecutor.rayTracePublic(player, 28.0);
        BlockPos base = BlockPos.containing(target);
        BlockPos ground = null;
        for (int i = 0; i <= 8; i++) {
            BlockPos probe = base.below(i);
            BlockState state = level.getBlockState(probe);
            if (state.isSolid() && state.getDestroySpeed(level, probe) >= 0.0F) {
                ground = probe;
                break;
            }
        }
        if (ground == null) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "rumblefruit.no_ground").withStyle(net.minecraft.ChatFormatting.GRAY), true);
            return;
        }

        // carve the slab: 3x3 footprint, surface + two layers down; bedrock,
        // liquids and block entities stay behind
        List<BlockState> states = new ArrayList<>(EarthChunkEntity.GRID);
        List<BlockPos> carved = new ArrayList<>();
        int solid = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 0; dy <= 2; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos pos = ground.offset(dx, -dy, dz);
                    BlockState state = level.getBlockState(pos);
                    boolean take = !state.isAir() && state.getFluidState().isEmpty()
                            && state.getDestroySpeed(level, pos) >= 0.0F
                            && !state.hasBlockEntity();
                    states.add(take ? state : Blocks.AIR.defaultBlockState());
                    if (take) {
                        carved.add(pos);
                        solid++;
                    }
                }
            }
        }
        if (solid < 3) { // not enough world to grab (bedrock floor, water, air)
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "rumblefruit.no_ground").withStyle(net.minecraft.ChatFormatting.GRAY), true);
            return;
        }
        for (BlockPos pos : carved) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
        // БАМ: the earth CRACKS around the wound — real radial fissures,
        // not just particles
        crackEarth(level, ground, 5, 5);

        // the slab rises from the crater
        Vec3 from = Vec3.atCenterOf(ground).add(0.0, -1.0, 0.0);
        EarthChunkEntity.spawn(level, player, from, states, element.id());

        // rumble of tearing earth + everyone nearby FEELS the ground rip out
        level.playSound(null, target.x, target.y, target.z,
                SoundEvents.ROOTED_DIRT_BREAK, SoundSource.PLAYERS, 3.0F, 0.5F);
        level.playSound(null, target.x, target.y, target.z,
                SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 3.0F, 0.4F);
        level.playSound(null, target.x, target.y, target.z,
                SoundEvents.WARDEN_DIG, SoundSource.PLAYERS, 2.0F, 0.7F);
        net.neoforged.neoforge.network.PacketDistributor.sendToAllPlayers(
                new ImpactShakePacket(target.x, target.y, target.z, 0.9F));
        // the rip scars the sky above the crater — visible from far away
        FarFx.column(level, element.spark(), target.x, target.y, target.z, 40.0, 2.0, 2, 0.5);
    }

    // tears REAL radial fissures into the terrain: one-block grooves that
    // crawl outward from the wound — the ground literally cracks open
    public static void crackEarth(ServerLevel level, BlockPos center, int arms, int length) {
        java.util.Random random = new java.util.Random();
        for (int arm = 0; arm < arms; arm++) {
            double angle = arm * Math.PI * 2.0 / arms + random.nextDouble() * 0.6;
            int armLength = 2 + random.nextInt(length);
            double cx = center.getX(), cz = center.getZ();
            for (int d = 0; d < armLength; d++) {
                // the fissure wanders like real cracked earth
                cx += Math.cos(angle) + (random.nextDouble() - 0.5) * 0.8;
                cz += Math.sin(angle) + (random.nextDouble() - 0.5) * 0.8;
                BlockPos surface = surfaceAt(level, BlockPos.containing(cx, center.getY() + 2.0, cz));
                if (surface == null) {
                    continue;
                }
                BlockState state = level.getBlockState(surface);
                if (state.getDestroySpeed(level, surface) >= 0.0F && !state.hasBlockEntity()
                        && state.getFluidState().isEmpty()) {
                    level.setBlock(surface, Blocks.AIR.defaultBlockState(), 3);
                    level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(
                                    net.minecraft.core.particles.ParticleTypes.BLOCK, state),
                            surface.getX() + 0.5, surface.getY() + 1.0, surface.getZ() + 0.5,
                            8, 0.3, 0.3, 0.3, 0.1);
                }
            }
        }
    }

    private static BlockPos surfaceAt(ServerLevel level, BlockPos near) {
        for (int i = 0; i <= 4; i++) {
            BlockPos probe = near.below(i);
            if (level.getBlockState(probe).isSolid()) {
                return probe;
            }
        }
        return null;
    }
}
