package net.minecraft.world.entity.ai.attributes;

import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Set;

// vacuum fake of minecraft's AttributeModifier
public class AttributeModifier {
    public enum Operation {
        ADD_VALUE, ADD_MULTIPLIED_BASE, ADD_MULTIPLIED_TOTAL
    }

    public final ResourceLocation id;
    public final double amount;
    public final Operation operation;

    public AttributeModifier(ResourceLocation id, double amount, Operation operation) {
        this.id = id;
        this.amount = amount;
        this.operation = operation;
    }
}
