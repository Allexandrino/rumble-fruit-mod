package net.minecraft.network.protocol.common.custom;

import net.minecraft.resources.ResourceLocation;

// vacuum fake of the nested CustomPacketPayload.Type — the $ name matches the
// real nested class binary name so compiled packet records link against it
public class CustomPacketPayload$Type<T> {
    public final ResourceLocation id;

    public CustomPacketPayload$Type(ResourceLocation id) {
        this.id = id;
    }
}
