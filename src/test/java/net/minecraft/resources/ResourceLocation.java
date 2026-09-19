package net.minecraft.resources;

// vacuum fake of minecraft's ResourceLocation
public class ResourceLocation {
    private final String namespace;
    private final String path;

    private ResourceLocation(String namespace, String path) {
        this.namespace = namespace;
        this.path = path;
    }

    public static ResourceLocation fromNamespaceAndPath(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }

    public static ResourceLocation parse(String value) {
        String[] parts = value.split(":", 2);
        return parts.length == 2 ? new ResourceLocation(parts[0], parts[1])
                : new ResourceLocation("minecraft", value);
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }
}
