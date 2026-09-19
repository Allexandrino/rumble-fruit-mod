package net.minecraft.network.codec;

// vacuum fake of minecraft's StreamCodec
public interface StreamCodec<B, V> {
    static <B, V> StreamCodec<B, V> of(StreamEncoder<? super B, V> encoder,
                                       StreamDecoder<B, ? extends V> decoder) {
        return new StreamCodec<>() {
        };
    }
}
