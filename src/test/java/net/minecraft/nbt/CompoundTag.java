package net.minecraft.nbt;

import java.util.HashMap;
import java.util.Map;

// vacuum fake of minecraft's CompoundTag (booleans only — all our data uses them)
public class CompoundTag {
    private final Map<String, Boolean> bools = new HashMap<>();

    public void putBoolean(String key, boolean value) {
        bools.put(key, value);
    }

    public boolean getBoolean(String key) {
        return bools.getOrDefault(key, false);
    }
}
