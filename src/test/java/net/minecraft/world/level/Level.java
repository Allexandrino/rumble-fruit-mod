package net.minecraft.world.level;

import net.minecraft.world.entity.Entity;

// vacuum fake of minecraft's Level
public class Level {
    public final boolean isClientSide = false;
    private long gameTime = 0;
    public final java.util.List<String> sounds = new java.util.ArrayList<>();

    public enum ExplosionInteraction {
        NONE, BLOCK, MOB, TNT
    }

    public long getGameTime() {
        return gameTime;
    }

    public void setGameTime(long time) {
        this.gameTime = time;
    }

    public Explosion explode(Entity exploder, double x, double y, double z, float radius,
                             ExplosionInteraction interaction) {
        return new Explosion();
    }

    public net.minecraft.world.damagesource.DamageSources damageSources() {
        return new net.minecraft.world.damagesource.DamageSources();
    }

    public void playSound(net.minecraft.world.entity.player.Player except,
                          net.minecraft.core.BlockPos pos,
                          net.minecraft.sounds.SoundEvent sound,
                          net.minecraft.sounds.SoundSource source, float volume, float pitch) {
        sounds.add(sound.toString());
    }

    public void playSound(net.minecraft.world.entity.player.Player except,
                          double x, double y, double z,
                          net.minecraft.sounds.SoundEvent sound,
                          net.minecraft.sounds.SoundSource source, float volume, float pitch) {
        sounds.add(sound.toString());
    }

    public void playSound(net.minecraft.world.entity.player.Player except,
                          double x, double y, double z,
                          net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> sound,
                          net.minecraft.sounds.SoundSource source, float volume, float pitch) {
        sounds.add(sound.value().toString());
    }
}
