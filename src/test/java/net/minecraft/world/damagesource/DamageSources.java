package net.minecraft.world.damagesource;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

// vacuum fake of minecraft's DamageSources factory
public class DamageSources {
    public DamageSource playerAttack(Player player) {
        return new DamageSource("player");
    }

    public DamageSource indirectMagic(Entity attacker, Entity owner) {
        return new DamageSource("magic");
    }
}
