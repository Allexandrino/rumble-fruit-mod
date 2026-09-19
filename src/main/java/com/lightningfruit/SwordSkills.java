package com.lightningfruit;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// sword stance skills ("Крыло Ангела"):
// Z = Severing Strike (lightning wave along the ground), X = Heavenly Slash (leap + slam),
// C = Execution (x3 damage to the wounded), V = Angel Stance (parry + counterattack)
@EventBusSubscriber(modid = LightningFruitMod.MOD_ID)
public class SwordSkills {
    private static final Map<UUID, Long> PENDING_SLAM = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> EXECUTE_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> PARRY_UNTIL = new ConcurrentHashMap<>();

    // Z: a wave of golden lightning rolling forward along the ground
    public static void severingStrike(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        boolean holy = WingsData.isActive(player.getUUID());
        Vec3 dir = player.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
        Vec3 start = player.position();
        for (int i = 2; i <= 16; i += 2) {
            double x = start.x + dir.x * i;
            double z = start.z + dir.z * i;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
            ElectroBoltEntity.strike(level, x, y, z, player, holy);
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                    new net.minecraft.world.phys.AABB(x - 2.5, y - 1, z - 2.5, x + 2.5, y + 3, z + 2.5),
                    e -> e != player && e.isAlive())) {
                entity.hurt(level.damageSources().indirectMagic(player, player), holy ? 24.0F : 16.0F);
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
            }
        }
        level.playSound(null, start.x, start.y, start.z,
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 3.0F, 0.9F);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, start.x, start.y + 1, start.z, 40, 4.0, 0.5, 4.0, 0.1);
    }

    // X: leap into the air; the slam lands when the player touches down
    public static void heavenlySlash(ServerPlayer player) {
        Vec3 view = player.getLookAngle();
        player.push(view.x * 0.8, 1.1, view.z * 0.8);
        player.hurtMarked = true;
        PENDING_SLAM.put(player.getUUID(), player.level().getGameTime());
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, 1.5F, 1.3F);
    }

    // the landing slam: lightning crash + big AoE
    private static void slam(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        boolean holy = WingsData.isActive(player.getUUID());
        Vec3 pos = player.position();
        ElectroBoltEntity.strike(level, pos.x, pos.y, pos.z, player, holy);
        level.explode(null, pos.x, pos.y, pos.z, 3.0F,
                net.minecraft.world.level.Level.ExplosionInteraction.NONE);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(7.0),
                e -> e != player && e.isAlive())) {
            entity.hurt(level.damageSources().indirectMagic(player, player), holy ? 30.0F : 22.0F);
            Vec3 away = entity.position().subtract(pos).normalize().scale(1.5);
            entity.push(away.x, 0.6, away.z);
            entity.hurtMarked = true;
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y + 0.5, pos.z, 60, 4.0, 1.0, 4.0, 0.1);
        level.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 4.0F, 0.7F);
    }

    // C: Execution — for 12 seconds, wounded enemies (below half HP) take x3 damage
    public static void execution(ServerPlayer player) {
        EXECUTE_UNTIL.put(player.getUUID(), player.level().getGameTime() + 240);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.5F, 0.8F);
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                "lightningfruit.execution").withStyle(net.minecraft.ChatFormatting.RED), true);
    }

    public static float executionMultiplier(ServerPlayer player, LivingEntity target) {
        Long until = EXECUTE_UNTIL.get(player.getUUID());
        if (until == null || player.level().getGameTime() > until) {
            return 1.0F;
        }
        return target.getHealth() < target.getAttributeValue(Attributes.MAX_HEALTH) * 0.5F ? 3.0F : 1.0F;
    }

    // V: Angel Stance — for 3 seconds, incoming hits are parried and answered with lightning
    public static void angelStance(ServerPlayer player) {
        PARRY_UNTIL.put(player.getUUID(), player.level().getGameTime() + 60);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.5F, 1.4F);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.5F, 1.1F);
    }

    @SubscribeEvent
    public static void onPlayerHurt(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Long until = PARRY_UNTIL.get(player.getUUID());
        if (until == null || player.level().getGameTime() > until) {
            return;
        }
        event.setNewDamage(0.0F); // parried
        if (player.level() instanceof ServerLevel level
                && event.getSource().getEntity() instanceof LivingEntity attacker) {
            // counterattack: golden lightning answers the attacker
            ElectroBoltEntity.strike(level, attacker.getX(), attacker.getY(), attacker.getZ(), player,
                    WingsData.isActive(player.getUUID()));
            attacker.hurt(level.damageSources().indirectMagic(player, player), 25.0F);
            attacker.push((attacker.getX() - player.getX()), 0.6, (attacker.getZ() - player.getZ()));
            attacker.hurtMarked = true;
        }
        level_sound(player);
    }

    private static void level_sound(ServerPlayer player) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.2F, 1.4F);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Long since = PENDING_SLAM.get(player.getUUID());
        if (since != null) {
            if (player.onGround() && player.level().getGameTime() > since + 5) {
                PENDING_SLAM.remove(player.getUUID());
                slam(player);
            } else if (player.level().getGameTime() > since + 200) {
                PENDING_SLAM.remove(player.getUUID());
            }
        }
    }
}
