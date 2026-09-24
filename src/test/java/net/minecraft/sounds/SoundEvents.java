package net.minecraft.sounds;

// vacuum fake of minecraft's SoundEvents constants used by the mod
public class SoundEvents {
    public static final SoundEvent WARDEN_SONIC_CHARGE = new SoundEvent("warden_sonic_charge");
    public static final SoundEvent WARDEN_SONIC_BOOM = new SoundEvent("warden_sonic_boom");
    public static final SoundEvent BEACON_POWER_SELECT = new SoundEvent("beacon_power_select");
    public static final SoundEvent BEACON_AMBIENT = new SoundEvent("beacon_ambient");
    public static final SoundEvent BEACON_ACTIVATE = new SoundEvent("beacon_activate");
    public static final SoundEvent BEACON_DEACTIVATE = new SoundEvent("beacon_deactivate");
    public static final SoundEvent LIGHTNING_BOLT_THUNDER = new SoundEvent("thunder");
    public static final SoundEvent LIGHTNING_BOLT_IMPACT = new SoundEvent("bolt_impact");
    public static final SoundEvent ENDER_DRAGON_DEATH = new SoundEvent("dragon_death");
    public static final SoundEvent ENDER_DRAGON_GROWL = new SoundEvent("dragon_growl");
    public static final SoundEvent PORTAL_AMBIENT = new SoundEvent("portal_ambient");
    // this one is a Holder.Reference in 1.21, not a plain SoundEvent
    public static final net.minecraft.core.Holder.Reference<SoundEvent> GENERIC_EXPLODE =
            new net.minecraft.core.Holder.Reference<>() {
                @Override
                public SoundEvent value() {
                    return new SoundEvent("explode");
                }
            };
    public static final SoundEvent PLAYER_ATTACK_SWEEP = new SoundEvent("attack_sweep");
    public static final SoundEvent PLAYER_ATTACK_STRONG = new SoundEvent("attack_strong");
    public static final SoundEvent PLAYER_ATTACK_KNOCKBACK = new SoundEvent("attack_knockback");
    public static final SoundEvent ARROW_SHOOT = new SoundEvent("arrow_shoot");
    public static final SoundEvent ENDER_DRAGON_FLAP = new SoundEvent("dragon_flap");
    public static final SoundEvent TOTEM_USE = new SoundEvent("totem_use");
    public static final SoundEvent BLAZE_SHOOT = new SoundEvent("blaze_shoot");
    public static final SoundEvent PLAYER_HURT_FREEZE = new SoundEvent("hurt_freeze");
    public static final SoundEvent BONE_MEAL_USE = new SoundEvent("bone_meal");
}
