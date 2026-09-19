package net.minecraft.world.entity.ai.attributes;

import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Set;

// vacuum fake of minecraft's AttributeInstance
public class AttributeInstance {
    private final Set<ResourceLocation> modifiers = new HashSet<>();

    public AttributeModifier getModifier(ResourceLocation id) {
        return modifiers.contains(id) ? new AttributeModifier(id, 0.0, AttributeModifier.Operation.ADD_VALUE) : null;
    }

    public void addPermanentModifier(AttributeModifier modifier) {
        modifiers.add(modifier.id);
    }

    public void removeModifier(ResourceLocation id) {
        modifiers.remove(id);
    }
}
