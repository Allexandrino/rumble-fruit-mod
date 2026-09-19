package net.minecraft.core;

// vacuum fake of minecraft's Holder
public interface Holder<T> {
    T value();

    static <T> Holder<T> direct(T value) {
        return () -> value;
    }

    // nested like the real Holder.Reference
    interface Reference<T> extends Holder<T> {
    }
}
