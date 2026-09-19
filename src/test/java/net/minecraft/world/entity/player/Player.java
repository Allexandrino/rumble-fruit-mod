package net.minecraft.world.entity.player;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

// vacuum fake of minecraft's Player
public class Player extends LivingEntity {
    private final CompoundTag persistentData = new CompoundTag();
    public final List<String> messages = new ArrayList<>();
    private boolean usingItem = false;
    private int swingCount = 0;

    public CompoundTag getPersistentData() {
        return persistentData;
    }

    public void displayClientMessage(Component message, boolean actionBar) {
        messages.add(message.getString());
    }

    public AttributeInstance getAttribute(net.minecraft.core.Holder<Attribute> attribute) {
        return new AttributeInstance();
    }

    public boolean removeEffect(net.minecraft.core.Holder<MobEffect> effect) {
        return true;
    }

    private final Abilities abilities = new Abilities();

    public Abilities getAbilities() {
        return abilities;
    }

    public boolean isCreative() {
        return false;
    }

    public boolean isSpectator() {
        return false;
    }

    public void onUpdateAbilities() {
    }

    public void resetFallDistance() {
        this.fallDistance = 0.0F;
    }

    public void swing(InteractionHand hand, boolean updateSelf) {
        swingCount++;
    }

    public int getSwingCount() {
        return swingCount;
    }

    public boolean isUsingItem() {
        return usingItem;
    }

    public void startUsingItem(InteractionHand hand) {
        usingItem = true;
    }

    public void stopUsingItem() {
        usingItem = false;
    }

    public ItemStack getMainHandItem() {
        return ItemStack.EMPTY;
    }

    public Component getName() {
        return Component.translatable("FakePlayer");
    }
}
