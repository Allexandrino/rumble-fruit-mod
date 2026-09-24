package com.rumblefruit;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// the elemental skill kits: every element x every skill (Z/X/C/V) must have
// its own observable behavior — damage, riders, teleports, heals, meteors
class ElementSkillsTest {
    private ServerPlayer player;
    private ServerLevel level;
    private LivingEntity victim;

    @BeforeEach
    void setUp() {
        player = new ServerPlayer();
        level = new ServerLevel();
        player.setServerLevel(level);
        player.setPos(0.0, 64.0, 0.0);
        player.setYRot(0.0F);
        player.setXRot(0.0F);
        // the player looks along -z; the victim stands in the line of fire
        victim = new LivingEntity();
        victim.setPos(0.0, 64.0, -5.0);
        level.setQueryResult(java.util.List.of(victim));
        // targeted skills mark the ground right next to the victim
        level.setClipResult(new net.minecraft.world.phys.BlockHitResult(
                new net.minecraft.world.phys.Vec3(0.0, 64.0, -6.0),
                net.minecraft.world.phys.HitResult.Type.BLOCK));
    }

    private boolean somethingHappened() {
        return !level.particles.isEmpty() || !level.sounds.isEmpty() || !level.explosions.isEmpty()
                || !victim.damageLog.isEmpty() || !victim.effectLog.isEmpty()
                || !level.freshEntities.isEmpty();
    }

    @Test
    void everyElementalSkillDoesSomething() {
        int[] skills = {0, 1, 2, 5};
        for (Element element : new Element[]{Element.INFERNO, Element.VOID, Element.FROST, Element.NATURE}) {
            for (int skill : skills) {
                setUp();
                // when the skill fires
                ElementSkills.cast(element, player, skill, 1);
                // then the world noticed
                assertTrue(somethingHappened(), element + " skill " + skill + " was a dud");
            }
        }
    }

    @Test
    void infernoZIgnitesTheTarget() {
        // when the flame jet hits
        ElementSkills.cast(Element.INFERNO, player, 0, 1);
        // then the victim takes damage and catches fire
        assertFalse(victim.damageLog.isEmpty());
        assertTrue(victim.fireTicks > 0);
    }

    @Test
    void frostZDeepFreezesTheTarget() {
        // when the ice lance pierces
        ElementSkills.cast(Element.FROST, player, 0, 1);
        // then the victim is hurt, slowed and frozen solid
        assertFalse(victim.damageLog.isEmpty());
        assertTrue(victim.frozenTicks >= 200);
        assertFalse(victim.effectLog.isEmpty());
    }

    @Test
    void voidZBlinksTheCasterThroughTheTarget() {
        // when the shadow rift fires
        ElementSkills.cast(Element.VOID, player, 0, 1);
        // then the caster stands 12 blocks ahead (along -z), the victim shredded
        assertEquals(-12.0, player.getZ(), 0.1);
        assertFalse(victim.damageLog.isEmpty());
    }

    @Test
    void natureZDragsTheVictimClose() {
        // when the vine lash whips
        ElementSkills.cast(Element.NATURE, player, 0, 1);
        // then the victim is yanked towards the caster
        assertFalse(victim.damageLog.isEmpty());
        assertTrue(victim.getDeltaMovement().length() > 1.0);
    }

    @Test
    void natureXHealsTheCaster() {
        // given a wounded caster
        player.setHealth(10.0F);
        // when the bloom burst fires
        ElementSkills.cast(Element.NATURE, player, 1, 1);
        // then the caster mends
        assertTrue(player.getHealth() > 10.0F);
    }

    @Test
    void infernoVDropsABurningMeteor() {
        // when the inferno meteor falls
        ElementSkills.cast(Element.INFERNO, player, 5, 1);
        // then a burning block dives at the mark and the area detonates
        assertTrue(level.freshEntities.stream().anyMatch(e -> e instanceof net.minecraft.world.entity.item.FallingBlockEntity));
        assertFalse(level.explosions.isEmpty());
    }

    @Test
    void voidCFlingsTheMarkedIntoTheSky() {
        // when the void drop hits
        ElementSkills.cast(Element.VOID, player, 2, 1);
        // then the marked victim is hurled upwards
        assertFalse(victim.damageLog.isEmpty());
        assertTrue(victim.getDeltaMovement().y > 1.0);
    }

    @Test
    void frostCLeavesALineOfSpikes() {
        // when the ice spikes erupt
        ElementSkills.cast(Element.FROST, player, 2, 1);
        // then spikes glow along the whole line, not just at the target
        assertTrue(level.particles.size() >= 8);
    }

    @Test
    void natureCRootsTheMarked() {
        // when the root prison snaps shut
        ElementSkills.cast(Element.NATURE, player, 2, 1);
        // then the marked victim can barely crawl
        assertFalse(victim.effectLog.isEmpty());
        assertFalse(victim.damageLog.isEmpty());
    }

    @Test
    void transformedCasterCarriesEmpoweredRiders() {
        // given the transformation active
        WingsData.setActive(player, true);
        // when the flame jet hits
        ElementSkills.cast(Element.INFERNO, player, 0, 1);
        // then the burn is the empowered 200 ticks and damage is 60% up
        assertEquals(200, victim.fireTicks);
        assertEquals(14.0F * 1.6F, victim.damageLog.get(0), 0.01F);
        WingsData.setActive(player, false);
    }
}
