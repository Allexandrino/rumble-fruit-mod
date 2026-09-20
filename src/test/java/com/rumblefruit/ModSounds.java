package com.rumblefruit;

import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

// vacuum fake: shadows the real ModSounds (its DeferredRegister needs a live game)
public class ModSounds {
    public static final DeferredHolder<SoundEvent, SoundEvent> ELECTRO_ZAP =
            new DeferredHolder<>(new SoundEvent("electro_zap"));
    public static final DeferredHolder<SoundEvent, SoundEvent> ELECTRO_CHARGE =
            new DeferredHolder<>(new SoundEvent("electro_charge"));
    public static final DeferredHolder<SoundEvent, SoundEvent> ELECTRO_BLAST =
            new DeferredHolder<>(new SoundEvent("electro_blast"));
}
