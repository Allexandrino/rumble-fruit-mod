package net.minecraft.sounds;

// vacuum fake of minecraft's SoundEvent
public class SoundEvent {
    private final String name;

    public SoundEvent(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
