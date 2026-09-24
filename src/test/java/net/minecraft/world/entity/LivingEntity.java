package net.minecraft.world.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.List;

// vacuum fake of minecraft's LivingEntity — records hits and effects
public class LivingEntity extends Entity {
    private float health = 20.0F;
    private float maxHealth = 20.0F;
    public final List<Float> damageLog = new ArrayList<>();
    public final List<MobEffectInstance> effectLog = new ArrayList<>();

    public boolean hurt(DamageSource source, float amount) {
        damageLog.add(amount);
        health -= amount;
        return true;
    }

    public boolean addEffect(MobEffectInstance effect) {
        effectLog.add(effect);
        return true;
    }

    public void knockback(double strength, double dx, double dz) {
        push(dx * strength, 0.1 * strength, dz * strength);
    }

    public float getHealth() {
        return health;
    }

    public void setHealth(float health) {
        this.health = health;
    }

    public float getMaxHealth() {
        return maxHealth;
    }

    public void heal(float amount) {
        health = Math.min(maxHealth, health + amount);
    }
}
