package net.neoforged.neoforge.event.tick;

import net.minecraft.world.entity.player.Player;

// vacuum fake of neoforge's PlayerTickEvent
public abstract class PlayerTickEvent {
    private final Player player;

    protected PlayerTickEvent(Player player) {
        this.player = player;
    }

    public Player getEntity() {
        return player;
    }

    public static class Post extends PlayerTickEvent {
        public Post(Player player) {
            super(player);
        }
    }
}
