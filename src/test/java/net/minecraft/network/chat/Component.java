package net.minecraft.network.chat;

// vacuum fake of minecraft's Component
public interface Component {
    static MutableComponent translatable(String key) {
        return new MutableComponent(key);
    }

    String getString();
}
