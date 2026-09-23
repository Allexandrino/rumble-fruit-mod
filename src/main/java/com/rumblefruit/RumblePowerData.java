package com.rumblefruit;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;


// tracks whether a player has eaten the Lightning fruit and unlocked its powers.
// server-side truth lives in the player's persistent NBT (survives logout and death);
// clients get it pushed via PowerSyncPacket (see ClientPowerData).
@EventBusSubscriber(modid = RumbleFruitMod.MOD_ID)
public class RumblePowerData {
    private static final String TAG = "hasLightningPower";
    private static final String TAG_ELEMENT = "fruitElement";
    private static final net.minecraft.resources.ResourceLocation HP_MODIFIER_ID =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "hp_boost");

    // SERVER-side check only. on the client use ClientPowerData.has()
    public static boolean hasPower(Player player) {
        return player.getPersistentData().getBoolean(TAG);
    }

    // which fruit element the player carries (0 = lightning)
    public static int elementOf(Player player) {
        return player.getPersistentData().getInt(TAG_ELEMENT);
    }

    public static void grant(Player player) {
        grant(player, 0);
    }

    public static void grant(Player player, int element) {
        if (player.level().isClientSide) {
            return;
        }
        if (hasPower(player) && elementOf(player) == element) {
            return;
        }
        player.getPersistentData().putBoolean(TAG, true);
        player.getPersistentData().putInt(TAG_ELEMENT, element);
        // resistance + extra max HP so the user feels tougher
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, MobEffectInstance.INFINITE_DURATION, 1, false, false));
        var attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr != null && attr.getModifier(HP_MODIFIER_ID) == null) {
            attr.addPermanentModifier(new AttributeModifier(HP_MODIFIER_ID, 10.0, AttributeModifier.Operation.ADD_VALUE));
        }
        // restore the new hearts
        player.setHealth(player.getMaxHealth());
        syncToClient(player);
    }

    public static void revoke(Player player) {
        if (player.level().isClientSide || !hasPower(player)) {
            return;
        }
        player.getPersistentData().putBoolean(TAG, false);
        player.getPersistentData().putInt(TAG_ELEMENT, 0);
        player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        var attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr != null && attr.getModifier(HP_MODIFIER_ID) != null) {
            attr.removeModifier(HP_MODIFIER_ID);
        }
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
        syncToClient(player);
    }

    private static void syncToClient(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(serverPlayer,
                    new PowerSyncPacket(hasPower(serverPlayer), elementOf(serverPlayer)));
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        syncToClient(event.getEntity());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        syncToClient(event.getEntity());
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }
        CompoundTag original = event.getOriginal().getPersistentData();
        if (original.getBoolean(TAG)) {
            event.getEntity().getPersistentData().putBoolean(TAG, true);
            event.getEntity().getPersistentData().putInt(TAG_ELEMENT, original.getInt(TAG_ELEMENT));
            // re-apply resistance (attributes persist through clone, but effects don't)
            event.getEntity().addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,
                    MobEffectInstance.INFINITE_DURATION, 1, false, false));
        }
    }
}
