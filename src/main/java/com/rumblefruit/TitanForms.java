package com.rumblefruit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// titan transformations: while the fruit form (F) is active the elemental
// titans grow HUGE. The vanilla SCALE attribute does the heavy lifting — it
// syncs to every client by itself, so the giant body, the hitbox and the
// first-person eye height all agree (perfect for third-person play).
// every titan also MARKS THE WORLD: the demon scorches what stands near,
// the ice dragon freezes the water it walks on and breathes frost in
// flight, the void king withers, mother nature blooms. and every titan
// lands like a meteor: dive into the ground fast enough and the impact
// detonates in a shockwave.
@EventBusSubscriber(modid = RumbleFruitMod.MOD_ID)
public final class TitanForms {

    private static final ResourceLocation SCALE_MOD =
            ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "titan_scale");
    private static final ResourceLocation STEP_MOD =
            ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "titan_step");
    private static final ResourceLocation KNOCKBACK_MOD =
            ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "titan_knockback");
    private static final ResourceLocation FALL_MOD =
            ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "titan_safe_fall");
    private static final ResourceLocation REACH_MOD =
            ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "titan_reach");

    // how giant each form grows (1.0 = human size)
    public static double scaleOf(int elementId) {
        return switch (elementId) {
            case 1 -> 1.9;   // Демон-Пожиратель
            case 2 -> 1.8;   // Король Тьмы
            case 3 -> 1.9;   // Ледяной Дракон
            case 4 -> 1.8;   // Природа-Мать
            default -> 1.15; // Электро-Ангел: чуть выше, но остаётся стремительным
        };
    }

    private static void put(ServerPlayer player, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                            ResourceLocation id, double amount) {
        var instance = player.getAttribute(attribute);
        if (instance != null && !instance.hasModifier(id)) {
            instance.addTransientModifier(new AttributeModifier(id, amount,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    private static void clear(ServerPlayer player, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                              ResourceLocation id) {
        var instance = player.getAttribute(attribute);
        if (instance != null && instance.hasModifier(id)) {
            instance.removeModifier(id);
        }
    }

    // the transformation flash grows the body; called when the form opens
    public static void apply(ServerPlayer player) {
        int elementId = RumblePowerData.elementOf(player);
        double scale = scaleOf(elementId);
        put(player, Attributes.SCALE, SCALE_MOD, scale - 1.0);
        if (scale > 1.3) {
            // titans stride over full blocks, shrug off hits and reach far
            put(player, Attributes.STEP_HEIGHT, STEP_MOD, 0.6);
            put(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_MOD, 0.6);
            put(player, Attributes.SAFE_FALL_DISTANCE, FALL_MOD, 5.0);
            put(player, Attributes.BLOCK_INTERACTION_RANGE, REACH_MOD, 0.5);
            put(player, Attributes.ENTITY_INTERACTION_RANGE, REACH_MOD, 0.5);
        }
    }

    public static void remove(ServerPlayer player) {
        clear(player, Attributes.SCALE, SCALE_MOD);
        clear(player, Attributes.STEP_HEIGHT, STEP_MOD);
        clear(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_MOD);
        clear(player, Attributes.SAFE_FALL_DISTANCE, FALL_MOD);
        clear(player, Attributes.BLOCK_INTERACTION_RANGE, REACH_MOD);
        clear(player, Attributes.ENTITY_INTERACTION_RANGE, REACH_MOD);
    }

    // relog: the form state is session-transient, so a stale scale modifier
    // (saved on the player entity) must never survive a rejoin
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            remove(player);
            DIVE_SPEED.remove(player.getUUID());
        }
    }

    private static final Map<UUID, Double> DIVE_SPEED = new ConcurrentHashMap<>();

    // called every server tick from WingsData while the form is active
    public static void tick(ServerPlayer player, int elementId) {
        // respawn resets attributes: grow back if the form is still on
        if (player.level().getGameTime() % 20 == 0) {
            apply(player);
        }
        ServerLevel level = (ServerLevel) player.level();
        long now = level.getGameTime();

        switch (elementId) {
            case 1 -> demonTick(player, level, now);
            case 2 -> voidTick(player, level, now);
            case 3 -> dragonTick(player, level, now);
            case 4 -> natureTick(player, level, now);
            default -> {
            }
        }

        // every titan lands like a meteor: dive into the ground fast enough
        // (wings reset the fall counter, so we track the dive VELOCITY) and
        // the landing detonates in a shockwave
        double dy = player.getDeltaMovement().y;
        double dive = Math.min(DIVE_SPEED.getOrDefault(player.getUUID(), 0.0), dy);
        if (player.onGround()) {
            if (dive < -0.75) {
                stomp(player, level, (float) (-dive * 9.0F));
            }
            DIVE_SPEED.put(player.getUUID(), 0.0);
        } else {
            DIVE_SPEED.put(player.getUUID(), dive);
        }
    }

    // ---------------- demon (inferno) ----------------
    private static void demonTick(ServerPlayer player, ServerLevel level, long now) {
        // heat aura: everything standing close to the demon catches fire
        if (now % 20 == 0) {
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(4.0), e -> e != player && e.isAlive())) {
                e.setRemainingFireTicks(60);
            }
        }
        // smouldering footprints
        if (now % 2 == 0 && player.onGround() && player.getDeltaMovement().horizontalDistanceSqr() > 0.01) {
            level.sendParticles(ParticleTypes.FLAME,
                    player.getX(), player.getY() + 0.1, player.getZ(), 2, 0.4, 0.05, 0.4, 0.01);
            level.sendParticles(ParticleTypes.LAVA,
                    player.getX(), player.getY() + 0.2, player.getZ(), 1, 0.5, 0.1, 0.5, 0.0);
        }
    }

    // ---------------- void king ----------------
    private static void voidTick(ServerPlayer player, ServerLevel level, long now) {
        // aura of decay: the king's presence withers the living
        if (now % 20 == 0) {
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(5.0), e -> e != player && e.isAlive())) {
                e.addEffect(new MobEffectInstance(MobEffects.WITHER, 50, 0));
            }
            level.sendParticles(ParticleTypes.SOUL,
                    player.getX(), player.getY() + 1.2, player.getZ(), 6, 0.8, 1.0, 0.8, 0.02);
        }
    }

    // ---------------- ice dragon (frost) ----------------
    private static void dragonTick(ServerPlayer player, ServerLevel level, long now) {
        // frost walker: water freezes solid under the dragon's tread
        if (now % 3 == 0 && player.onGround()) {
            BlockPos center = player.blockPosition();
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-3, -1, -3), center.offset(3, 0, 3))) {
                if (level.getBlockState(pos).is(Blocks.WATER)) {
                    level.setBlockAndUpdate(pos.immutable(), Blocks.FROSTED_ICE.defaultBlockState());
                }
            }
        }
        // frost breath: while the wings drive the dragon forward it exhales a
        // freezing cone that chills everything caught inside
        if (now % 3 == 0 && !player.onGround()
                && player.getDeltaMovement().horizontalDistanceSqr() > 0.3) {
            net.minecraft.world.phys.Vec3 eye = player.getEyePosition();
            net.minecraft.world.phys.Vec3 view = player.getLookAngle();
            for (double d = 1.5; d < 9.0; d += 1.2) {
                net.minecraft.world.phys.Vec3 p = eye.add(view.scale(d));
                level.sendParticles(Element.FROST.spark(), p.x, p.y - 0.6, p.z,
                        3, 0.25, 0.25, 0.25, 0.02);
                level.sendParticles(ParticleTypes.SNOWFLAKE, p.x, p.y - 0.6, p.z,
                        2, 0.3, 0.3, 0.3, 0.01);
            }
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().expandTowards(view.scale(9.0)).inflate(2.0),
                    e -> e != player && e.isAlive())) {
                net.minecraft.world.phys.Vec3 to = e.position().subtract(eye);
                double along = to.dot(view);
                if (along > 1.0 && along < 10.0
                        && eye.add(view.scale(along)).distanceTo(e.position()) < 2.5) {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
                    e.setTicksFrozen(Math.min(e.getTicksFrozen() + 60, 300));
                }
            }
        }
    }

    // ---------------- mother nature ----------------
    private static void natureTick(ServerPlayer player, ServerLevel level, long now) {
        // life aura: slow mend + the ground blossoms where she walks
        if (now % 20 == 0) {
            player.heal(1.0F);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    player.getX(), player.getY() + 1.4, player.getZ(), 5, 1.0, 1.0, 1.0, 0.0);
        }
        if (now % 7 == 0 && player.onGround()) {
            BlockPos under = player.blockPosition().below();
            BlockPos at = player.blockPosition();
            BlockState ground = level.getBlockState(under);
            BlockState spot = level.getBlockState(at);
            if (ground.is(Blocks.GRASS_BLOCK) && spot.isAir()
                    && level.random.nextInt(3) == 0) {
                BlockState flower = switch (level.random.nextInt(4)) {
                    case 0 -> Blocks.POPPY.defaultBlockState();
                    case 1 -> Blocks.DANDELION.defaultBlockState();
                    default -> Blocks.SHORT_GRASS.defaultBlockState();
                };
                level.setBlockAndUpdate(at, flower);
            }
        }
    }

    // titan landing: the impact crater — damage, dust in the ground's own
    // material, a thud you feel; the caster is shielded for the frame
    private static void stomp(ServerPlayer player, ServerLevel level, float force) {
        double radius = Math.min(8.0, 3.0 + force * 0.35);
        float damage = Math.min(40.0F, 8.0F + force * 2.2F);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 6, 4, false, false));
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(radius), e -> e != player && e.isAlive())) {
            double dist = e.position().distanceTo(player.position());
            if (dist > radius) {
                continue;
            }
            e.hurt(level.damageSources().indirectMagic(player, player), damage);
            Element.byId(RumblePowerData.elementOf(player)).applyRider(e, player, true);
            net.minecraft.world.phys.Vec3 away = e.position().subtract(player.position())
                    .multiply(1.0, 0.0, 1.0).normalize().scale(1.2);
            e.push(away.x, 0.7, away.z);
            e.hurtMarked = true;
        }
        BlockState ground = level.getBlockState(player.blockPosition().below());
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground),
                player.getX(), player.getY() + 0.3, player.getZ(),
                120, radius * 0.5, 0.4, radius * 0.5, 0.25);
        level.explode(null, player.getX(), player.getY(), player.getZ(), 2.0F,
                net.minecraft.world.level.Level.ExplosionInteraction.NONE);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.5F, 0.7F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.5F, 0.5F);
        // everyone nearby FEELS the giant land — hard camera shake
        net.neoforged.neoforge.network.PacketDistributor.sendToAllPlayers(
                new ImpactShakePacket(player.getX(), player.getY(), player.getZ(), 1.2F));
        // the ground SPLITS under the titan's weight
        crackAround(player, level);
        // everyone nearby watches the giant land — play the landing animation
        net.neoforged.neoforge.network.PacketDistributor.sendToAllPlayers(
                new CombatAnimPacket(player.getUUID(), 21));
    }

    // real fissures crawling out from under the landing titan
    private static void crackAround(ServerPlayer player, ServerLevel level) {
        TerraSkills.crackEarth(level, player.blockPosition(), 5, 4);
    }
}
