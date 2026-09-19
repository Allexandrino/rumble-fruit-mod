package com.lightningfruit;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

import java.util.UUID;

// makes hostile mobs tougher so they are worthy opponents for a Lightning fruit user
@EventBusSubscriber(modid = LightningFruitMod.MOD_ID)
public class MonsterBuff {
    private static final net.minecraft.resources.ResourceLocation HP_ID = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(LightningFruitMod.MOD_ID, "worthy_hp");
    private static final net.minecraft.resources.ResourceLocation DMG_ID = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(LightningFruitMod.MOD_ID, "worthy_dmg");
    private static final net.minecraft.resources.ResourceLocation SPD_ID = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(LightningFruitMod.MOD_ID, "worthy_spd");

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }
        if (!entity.getType().getCategory().isFriendly()) {
            return; // only hostile mobs
        }
        var hp = entity.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null && hp.getModifier(HP_ID) == null) {
            hp.addPermanentModifier(new AttributeModifier(HP_ID, 1.0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            entity.setHealth(entity.getMaxHealth());
        }
        var dmg = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null && dmg.getModifier(DMG_ID) == null) {
            dmg.addPermanentModifier(new AttributeModifier(DMG_ID, 0.75,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        var spd = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (spd != null && spd.getModifier(SPD_ID) == null) {
            spd.addPermanentModifier(new AttributeModifier(SPD_ID, 0.15,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }
}
