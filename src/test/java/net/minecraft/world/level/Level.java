package net.minecraft.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

// vacuum fake of minecraft's Level
public class Level implements BlockGetter {
    public final boolean isClientSide = false;
    private long gameTime = 0;

    // structured record of a played sound — tests assert real parameters
    public static class SoundCall {
        public final SoundEvent sound;
        public final SoundSource source;
        public final float volume;
        public final float pitch;

        public SoundCall(SoundEvent sound, SoundSource source, float volume, float pitch) {
            this.sound = sound;
            this.source = source;
            this.volume = volume;
            this.pitch = pitch;
        }
    }

    public final java.util.List<SoundCall> sounds = new java.util.ArrayList<>();

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
                          BlockPos pos, SoundEvent sound, SoundSource source,
                          float volume, float pitch) {
        sounds.add(new SoundCall(sound, source, volume, pitch));
    }

    public void playSound(net.minecraft.world.entity.player.Player except,
                          double x, double y, double z,
                          SoundEvent sound, SoundSource source, float volume, float pitch) {
        sounds.add(new SoundCall(sound, source, volume, pitch));
    }

    public void playSound(net.minecraft.world.entity.player.Player except,
                          double x, double y, double z,
                          net.minecraft.core.Holder<SoundEvent> sound,
                          SoundSource source, float volume, float pitch) {
        sounds.add(new SoundCall(sound.value(), source, volume, pitch));
    }
}
