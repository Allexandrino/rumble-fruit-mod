package net.minecraft.world.item;

// vacuum fake of minecraft's ItemStack
public class ItemStack {
    public static final ItemStack EMPTY = new ItemStack();

    public boolean isEmpty() {
        return this == EMPTY;
    }
}
