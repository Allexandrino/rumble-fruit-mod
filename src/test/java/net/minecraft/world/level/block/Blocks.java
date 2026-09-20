package net.minecraft.world.level.block;

// vacuum fake of minecraft's Blocks registry constants
public class Blocks {
    public static final Block AIR = new Block(false, true);
    public static final Block STONE = new Block(false, false);
    public static final Block BEDROCK = new Block(true, false);
    public static final Block COMMAND_BLOCK = new Block(true, false);
    public static final Block BARRIER = new Block(true, false);
    public static final Block STRUCTURE_BLOCK = new Block(true, false);
}
