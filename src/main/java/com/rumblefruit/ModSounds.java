package com.rumblefruit;

import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// custom electro sounds for skills — no vanilla thunder
public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT, RumbleFruitMod.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> ELECTRO_ZAP =
            SOUNDS.register("electro_zap", () -> SoundEvent.createVariableRangeEvent(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "electro_zap")));
    public static final DeferredHolder<SoundEvent, SoundEvent> ELECTRO_CHARGE =
            SOUNDS.register("electro_charge", () -> SoundEvent.createVariableRangeEvent(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "electro_charge")));
    public static final DeferredHolder<SoundEvent, SoundEvent> ELECTRO_BLAST =
            SOUNDS.register("electro_blast", () -> SoundEvent.createVariableRangeEvent(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "electro_blast")));
}
