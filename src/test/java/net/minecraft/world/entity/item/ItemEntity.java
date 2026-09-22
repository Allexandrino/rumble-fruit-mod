package net.minecraft.world.entity.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// vacuum fake of minecraft's ItemEntity — a dropped item with a stack inside
public class ItemEntity extends net.minecraft.world.entity.Entity {
    private final ItemStack stack;

    public ItemEntity(Level level, double x, double y, double z, ItemStack stack) {
        setLevel(level);
        setPos(x, y, z);
        this.stack = stack;
    }

    public ItemStack getItem() {
        return stack;
    }
}
