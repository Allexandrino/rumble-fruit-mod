package net.minecraft.nbt;

import java.util.HashMap;
import java.util.Map;

// vacuum fake of minecraft's CompoundTag (booleans + ints — all our data)
public class CompoundTag {
    private final Map<String, Boolean> bools = new HashMap<>();
    private final Map<String, Integer> ints = new HashMap<>();

    public void putBoolean(String key, boolean value) {
        bools.put(key, value);
    }

    public boolean getBoolean(String key) {
        return bools.getOrDefault(key, false);
    }

    public void putInt(String key, int value) {
        ints.put(key, value);
    }

    public int getInt(String key) {
        return ints.getOrDefault(key, 0);
    }
}
