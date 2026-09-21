package net.minecraft.server.level;

import net.minecraft.world.entity.player.Player;

// vacuum fake of minecraft's ServerPlayer
public class ServerPlayer extends Player {
    private ServerLevel serverLevel = new ServerLevel();

    @Override
    public ServerLevel level() {
        return serverLevel;
    }

    public void setServerLevel(ServerLevel level) {
        this.serverLevel = level;
        if (!level.players().contains(this)) {
            level.players().add(this);
        }
    }
}
