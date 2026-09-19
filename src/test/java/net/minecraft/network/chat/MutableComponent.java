package net.minecraft.network.chat;

// vacuum fake of minecraft's MutableComponent
public class MutableComponent implements Component {
    private final String text;

    public MutableComponent(String text) {
        this.text = text;
    }

    @Override
    public String getString() {
        return text;
    }

    public MutableComponent withStyle(net.minecraft.ChatFormatting format) {
        return this;
    }
}
