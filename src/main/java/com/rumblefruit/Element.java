package com.rumblefruit;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

// the five fruit elements: particle flavor, cast sound, transformation title
// and the elemental rider every skill hit carries
public enum Element {
    LIGHTNING(0),
    INFERNO(1),
    VOID(2),
    FROST(3),
    NATURE(4);

    private final int id;

    Element(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public com.rumblefruit.core.ElementCatalog catalog() {
        return com.rumblefruit.core.ElementCatalog.byId(id);
    }

    public static Element byId(int id) {
        return values()[Math.floorMod(id, 5)];
    }

    public net.minecraft.core.particles.SimpleParticleType spark() {
        return switch (this) {
            case LIGHTNING -> ModParticles.ELECTRO_SPARK.get();
            case INFERNO -> ModParticles.INFERNO_SPARK.get();
            case VOID -> ModParticles.VOID_SPARK.get();
            case FROST -> ModParticles.FROST_SPARK.get();
            case NATURE -> ModParticles.NATURE_SPARK.get();
        };
    }

    public SoundEvent castSound() {
        return switch (this) {
            case LIGHTNING -> SoundEvents.LIGHTNING_BOLT_THUNDER;
            case INFERNO -> SoundEvents.BLAZE_SHOOT;
            case VOID -> SoundEvents.WARDEN_SONIC_BOOM;
            case FROST -> SoundEvents.PLAYER_HURT_FREEZE;
            case NATURE -> SoundEvents.BONE_MEAL_USE;
        };
    }

    // the rider every skill hit carries: fire burns, void withers, frost
    // freezes, nature poisons and feeds the caster
    public void applyRider(LivingEntity target, LivingEntity caster) {
        applyRider(target, caster, false);
    }

    // empowered variant while the transformation (F) is active: riders burn
    // longer and hit a level higher
    public void applyRider(LivingEntity target, LivingEntity caster, boolean empowered) {
        switch (this) {
            case LIGHTNING -> {
                if (empowered) {
                    target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0));
                }
            }
            case INFERNO -> target.setRemainingFireTicks(empowered ? 200 : 100);
            case VOID -> {
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, empowered ? 120 : 60, empowered ? 2 : 1));
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, empowered ? 100 : 60, 0));
            }
            case FROST -> {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, empowered ? 120 : 80, empowered ? 3 : 2));
                target.setTicksFrozen(empowered ? 320 : 200);
            }
            case NATURE -> {
                target.addEffect(new MobEffectInstance(MobEffects.POISON, empowered ? 120 : 80, empowered ? 2 : 1));
                caster.heal(empowered ? 4.0F : 2.0F);
            }
        }
    }
}
