package com.lightningfruit;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

// epic electro bow: fires electric projectiles instead of arrows.
// requires the Lightning fruit power; created with the G key.
public class ElectroBowItem extends BowItem {
    public ElectroBowItem() {
        super(new Properties().durability(800).rarity(Rarity.EPIC));
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) {
            return;
        }
        if (!LightningPowerData.hasPower(player)) {
            return;
        }
        int charge = this.getUseDuration(stack, player) - timeLeft;
        float power = BowItem.getPowerForTime(charge);
        if (power < 0.1F) {
            return;
        }
        if (!level.isClientSide) {
            ElectroArrowEntity arrow = new ElectroArrowEntity(level, player);
            // normal bow ballistics: speed scales with draw time like a vanilla bow
            arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, power * 3.0F, 1.0F);
            level.addFreshEntity(arrow);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);
            player.getCooldowns().addCooldown(this, 10);
        }
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return false;
    }
}
