package com.rumblefruit;

import dev.kosmx.playerAnim.api.IPlayable;
import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// bridges the combat system into Player Animator: every CombatAnimPacket
// combo code maps to a keyframed body animation (sword combo, fist strikes,
// skill casts, the R release arc, the slow-mo fall and the crater landing).
// files live in assets/rumblefruit/player_animations/ and are hot-reloadable
public final class PlayerSkillAnimations {
    private static final Map<Integer, String> BY_COMBO = new HashMap<>();
    private static final Map<String, IPlayable> CACHE = new HashMap<>();
    // one animation layer per player, attached to their Player Animator stack
    private static final Map<UUID, ModifierLayer<IAnimation>> LAYERS = new HashMap<>();

    static {
        BY_COMBO.put(0, "sword_slash_0");
        BY_COMBO.put(1, "sword_slash_1");
        BY_COMBO.put(2, "sword_slash_2");
        BY_COMBO.put(9, "release_r");
        BY_COMBO.put(10, "fist_jab");
        BY_COMBO.put(11, "fist_cross");
        BY_COMBO.put(12, "fist_uppercut");
        BY_COMBO.put(13, "fist_kick");
        BY_COMBO.put(20, "fall");
        BY_COMBO.put(21, "land");
        BY_COMBO.put(30, "skill_z");
        BY_COMBO.put(31, "skill_x");
        BY_COMBO.put(32, "skill_c");
        BY_COMBO.put(33, "skill_v");
        BY_COMBO.put(34, "skill_terra");
    }

    private PlayerSkillAnimations() {
    }

    public static void play(UUID playerId, int combo) {
        String name = BY_COMBO.get(combo);
        if (name == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Player player = mc.level.getPlayerByUUID(playerId);
        if (!(player instanceof AbstractClientPlayer clientPlayer)) {
            return;
        }
        IPlayable playable = CACHE.computeIfAbsent(name,
                n -> PlayerAnimationRegistry.getAnimation(
                        ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, n)));
        if (!(playable instanceof KeyframeAnimation animation)) {
            return;
        }
        AnimationStack stack = PlayerAnimationAccess.getPlayerAnimLayer(clientPlayer);
        ModifierLayer<IAnimation> layer = LAYERS.computeIfAbsent(playerId, id -> {
            ModifierLayer<IAnimation> created = new ModifierLayer<>();
            stack.addAnimLayer(2500, created);
            return created;
        });
        layer.setAnimation(new KeyframeAnimationPlayer(animation));
    }
}
