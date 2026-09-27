package com.rumblefruit;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

// the guardian crystal: a burning violet crystal block on the cavern walls.
// breakable, glows, shields the Fallen Exorcist while it stands
public class GuardianCrystalBlock extends Block {
    public GuardianCrystalBlock() {
        super(Properties.of()
                .strength(2.0F)
                .lightLevel(state -> 15)
                .sound(SoundType.AMETHYST));
    }
}
