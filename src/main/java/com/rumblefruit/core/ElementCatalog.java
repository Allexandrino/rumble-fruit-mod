package com.rumblefruit.core;

// the elemental catalog: every fruit element in the mod. pure data and pure
// logic (no minecraft classes) so the whole table is unit-testable in vacuum
public enum ElementCatalog {
    LIGHTNING(0, "lightning", 0x7FD4FF),
    INFERNO(1, "inferno", 0xFF7A2A),
    VOID(2, "void", 0xB46CFF),
    FROST(3, "frost", 0xBFEFFF),
    NATURE(4, "nature", 0x7CFF6B);

    public static final int COUNT = 5;

    private final int id;
    private final String key;
    private final int color;

    ElementCatalog(int id, String key, int color) {
        this.id = id;
        this.key = key;
        this.color = color;
    }

    public int id() {
        return id;
    }

    public String key() {
        return key;
    }

    public int color() {
        return color;
    }

    // any int maps onto a valid element: out-of-range ids wrap around
    public static ElementCatalog byId(int id) {
        return values()[Math.floorMod(id, COUNT)];
    }

    // the fruit cycle: eating fruits moves you through the catalog
    public ElementCatalog next() {
        return byId(id + 1);
    }

    public boolean isLightning() {
        return this == LIGHTNING;
    }
}
