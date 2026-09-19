package com.rumblefruit;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

import java.util.Random;

// the electro sword: a vanilla-looking sword charged with lightning.
// requires the Lightning fruit power to use its abilities.
public class ElectroSwordItem extends SwordItem {
    private static final Tier ELECTRO_TIER = new Tier() {
        @Override public int getUses() { return 2000; }
        @Override public float getSpeed() { return 9.0F; }
        @Override public float getAttackDamageBonus() { return 8.0F; }
        @Override public net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> getIncorrectBlocksForDrops() {
            return net.minecraft.tags.BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
        }
        @Override public int getEnchantmentValue() { return 15; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.EMPTY; }
    };

    private final Random random = new Random();

    public ElectroSwordItem() {
        super(ELECTRO_TIER, new Properties().stacksTo(1).rarity(Rarity.EPIC)
                .attributes(SwordItem.createAttributes(ELECTRO_TIER, 0.0F, -2.2F)));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (!attacker.level().isClientSide && attacker.level() instanceof ServerLevel serverLevel
                && attacker instanceof Player player && RumblePowerData.hasPower(player)) {
            if (player.getCooldowns().isOnCooldown(this)) {
                return result;
            }
            target.hurt(serverLevel.damageSources().indirectMagic(player, player), 6.0F);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 15, 0));
            serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS,
                    1.0F, 1.0F + random.nextFloat() * 0.2F);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    9, 0.4, 0.4, 0.4, 0.05);
            if (random.nextFloat() < 0.1F) {
                ElectroBolts.strike(serverLevel, target.getX(), target.getY(), target.getZ(),
                        player instanceof net.minecraft.server.level.ServerPlayer sp ? sp : null);
            }
            player.getCooldowns().addCooldown(this, 15);
        }
        return result;
    }
}
