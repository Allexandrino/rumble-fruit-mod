package com.rumblefruit;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

// the titan forms as MONUMENTAL HoMM3-style creatures: a colossal elemental
// avatar (~23 blocks tall) towers around the player, who pilots it from its
// feet — susanoo-style. the player keeps his human hitbox (fully playable);
// the monument is a client-side visual manifestation.
//   lightning: TITAN (HoMM3 Tower) — spectral prismarine giant wrapped in
//              three storm rings of sea-lantern cubes
//   inferno:   EFREET (HoMM3 Inferno) — magma colossus whose legs are a
//              rotating fire vortex; a burning core pulses in its chest
//   void:      PSYCHIC ELEMENTAL (HoMM3 Conflux) — floating obsidian monolith,
//              detached floating hands, amethyst heart, orbiting debris rings
//   frost:     ICE ELEMENTAL (HoMM3 Conflux) — jagged blue-ice crystal giant,
//              shard crown orbiting its head
//   nature:    EARTH ELEMENTAL (HoMM3 Conflux) — mossy-cobblestone golem with
//              boulder shoulders and rooted-dirt legs
// every monument is built from ONE 16px unit cube (full-texture faces) with
// vanilla block textures, tinted per part. the giant walks with the player's
// gait: heavy slow swings, a breathing sway, arms counter-phased to legs.
//
// BUFFER DISCIPLINE (hard-won): the entity BufferSource ENDS the previous
// builder whenever the render type changes — a VertexConsumer held across
// another getBuffer() is a stale handle and crashes the frame ("Not
// building!"). every prop/ring fetches its consumer immediately before use.
public class TitanPropsLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "titan_props"), "main");

    private static final ResourceLocation SEA_LANTERN = vanilla("textures/block/sea_lantern.png");
    private static final ResourceLocation PRISMARINE = vanilla("textures/block/prismarine_bricks.png");
    private static final ResourceLocation MAGMA = vanilla("textures/block/magma.png");
    private static final ResourceLocation OBSIDIAN = vanilla("textures/block/obsidian.png");
    private static final ResourceLocation AMETHYST = vanilla("textures/block/amethyst_block.png");
    private static final ResourceLocation BLUE_ICE = vanilla("textures/block/blue_ice.png");
    private static final ResourceLocation ICE = vanilla("textures/block/ice.png");
    private static final ResourceLocation MOSSY = vanilla("textures/block/mossy_cobblestone.png");
    private static final ResourceLocation ROOTED = vanilla("textures/block/rooted_dirt.png");

    // monument proportions, blocks
    private static final float HIP_Y = 11.0F;      // legs 0..11
    private static final float TORSO_Y = 15.0F;    // torso 11..19 (8 tall, 8 wide, 4 deep)
    private static final float SHOULDER_Y = 18.6F;
    private static final float HEAD_Y = 21.2F;     // head ~19..23.4

    private final ModelPart cube;

    private static ResourceLocation vanilla(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }

    public TitanPropsLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                           EntityModelSet modelSet) {
        super(parent);
        this.cube = modelSet.bakeLayer(LAYER_LOCATION).getChild("cube");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        // 16px cube centered on the origin: every face samples the whole texture
        mesh.getRoot().addOrReplaceChild("cube",
                CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -8.0F, -8.0F, 16.0F, 16.0F, 16.0F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 16, 16);
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buffer, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!ClientWingsData.isActive(player.getUUID())) {
            return;
        }
        float unfold = Math.max(0.05F, ClientWingsData.unfoldProgress(player.getUUID(), ageInTicks));
        int elementId = com.rumblefruit.core.ElementCatalog.byId(ClientPowerData.element()).id();

        // the monument stands in ENTITY space: feet at the player's feet,
        // facing where he faces. the entity render frame is Y-DOWN (vanilla
        // flips it with scale(-1,-1,1)) and its origin sits at head height —
        // flip it back to Y-up and drop the origin to the feet (1.5 blocks
        // above the soles), then author the giant with +Y = up, in blocks
        float gait = Mth.sin(limbSwing * 0.5F) * limbSwingAmount;
        ps.pushPose();
        ps.scale(1.0F, -1.0F, 1.0F);
        ps.translate(0.0F, -1.5F, 0.0F);
        float breathe = 1.0F + Mth.sin(ageInTicks * 0.07F) * 0.008F;
        ps.scale(unfold * breathe, unfold * breathe, unfold * breathe);
        ps.mulPose(Axis.ZP.rotationDegrees(Mth.sin(ageInTicks * 0.045F) * 1.2F));
        switch (elementId) {
            case 0 -> renderTitan(ps, buffer, packedLight, gait, ageInTicks);
            case 1 -> renderEfreet(ps, buffer, packedLight, gait, ageInTicks);
            case 2 -> renderPsychic(ps, buffer, packedLight, gait, ageInTicks);
            case 3 -> renderIceElemental(ps, buffer, packedLight, gait, ageInTicks);
            case 4 -> renderEarthElemental(ps, buffer, packedLight, gait, ageInTicks);
            default -> {
            }
        }
        ps.popPose();
        ambience(player, elementId);
    }

    // ---------------- TITAN (lightning) — HoMM3 Tower ----------------
    private void renderTitan(PoseStack ps, MultiBufferSource buffer, int light, float gait, float age) {
        int spectral = 0xE8DCF2FF; // pale storm-cyan, mostly opaque
        legs(ps, buffer, PRISMARINE, true, light, gait, spectral);
        torso(ps, buffer, PRISMARINE, true, light, spectral);
        arms(ps, buffer, PRISMARINE, true, light, gait, spectral);
        head(ps, buffer, PRISMARINE, true, light, spectral, 4.5F);
        // storm rings coil the torso — scaled to the monument
        ringB(ps, buffer, SEA_LANTERN, true, light, 6.5F, 16, 0.9F, age * 0.045F, 0.0F, 0.0F, TORSO_Y, 0xFF8FE3FF);
        ringB(ps, buffer, SEA_LANTERN, true, light, 7.5F, 16, 0.75F, -age * 0.032F, 0.9F, 0.0F, TORSO_Y, 0xFFB8EEFF);
        ringB(ps, buffer, SEA_LANTERN, true, light, 8.5F, 20, 0.6F, age * 0.021F, -0.6F, 0.6F, TORSO_Y, 0xFF6FCFFF);
        // glowing core
        float pulse = 1.6F + Mth.sin(age * 0.13F) * 0.35F;
        propB(ps, buffer, SEA_LANTERN, true, light, 0.0F, TORSO_Y + 0.8F, 1.9F, pulse, pulse, 0.9F, 0xFF8FE3FF);
    }

    // ---------------- EFREET (inferno) — HoMM3 Inferno ----------------
    private void renderEfreet(PoseStack ps, MultiBufferSource buffer, int light, float gait, float age) {
        torso(ps, buffer, MAGMA, false, light, 0xFFFFFFFF);
        arms(ps, buffer, MAGMA, false, light, gait * 1.2F, 0xFFFFFFFF); // bigger swings — fire doesn't walk, it looms
        head(ps, buffer, MAGMA, false, light, 0xFFFFFFFF, 4.2F);
        // no legs: a rotating fire vortex carries the body
        for (int i = 0; i < 6; i++) {
            float y = 1.2F + i * 1.8F;
            float w = 8.5F - i * 1.35F;
            ps.pushPose();
            ps.translate(0.0F, y, 0.0F);
            ps.mulPose(Axis.YP.rotation(age * 0.09F + i * 0.8F));
            VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(MAGMA));
            ps.scale(w, 1.6F, w);
            cube.render(ps, vc, light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
            ps.popPose();
        }
        // the burning heart, seen through the torso
        float pulse = 2.4F + Mth.sin(age * 0.15F) * 0.6F;
        propB(ps, buffer, MAGMA, true, light, 0.0F, TORSO_Y + 0.6F, 2.0F, pulse, pulse, 1.0F, 0xFFFFC37A);
        // ember crown: small floating rocks over the head
        ringB(ps, buffer, MAGMA, true, light, 3.4F, 6, 0.7F, age * 0.06F, 0.35F, 0.0F, HEAD_Y + 3.4F, 0xFFFFA64D);
    }

    // ---------------- PSYCHIC ELEMENTAL (void) — HoMM3 Conflux ----------------
    private void renderPsychic(PoseStack ps, MultiBufferSource buffer, int light, float gait, float age) {
        float hover = 2.0F + Mth.sin(age * 0.06F) * 0.5F; // it never touches the ground
        ps.pushPose();
        ps.translate(0.0F, hover, 0.0F);
        // the monolith body: tall, narrow, faceless — no legs at all
        propB(ps, buffer, OBSIDIAN, false, light, 0.0F, 10.5F, 0.0F, 6.5F, 13.0F, 4.0F, 0xFFFFFFFF);
        propB(ps, buffer, OBSIDIAN, false, light, 0.0F, 18.2F, 0.0F, 4.2F, 3.2F, 3.6F, 0xFFFFFFFF);
        // the amethyst heart burning inside the slab
        float pulse = 2.2F + Mth.sin(age * 0.12F) * 0.55F;
        propB(ps, buffer, AMETHYST, true, light, 0.0F, 12.0F, 2.05F, pulse, pulse, 0.8F, 0xFFB46CFF);
        // detached hands floating beside the body, orbiting slowly
        for (int side = -1; side <= 1; side += 2) {
            double a = age * 0.05F + (side > 0 ? Math.PI : 0.0);
            propB(ps, buffer, OBSIDIAN, false, light,
                    (float) (side * 6.0 + Math.sin(a) * 0.8), 11.5F + Mth.sin(age * 0.09F + side) * 0.9F,
                    (float) (Math.cos(a) * 1.2), 2.6F, 2.6F, 2.6F, 0xFF2A2438);
        }
        // debris rings: orbiting shattered obsidian
        ringB(ps, buffer, OBSIDIAN, false, light, 6.5F, 9, 0.85F, age * 0.02F, 0.0F, 0.0F, 12.0F, 0xFF241F33);
        ringB(ps, buffer, OBSIDIAN, false, light, 8.0F, 7, 0.65F, -age * 0.028F, 0.6F, 0.0F, 12.0F, 0xFF3A2F55);
        ps.popPose();
    }

    // ---------------- ICE ELEMENTAL (frost) — HoMM3 Conflux ----------------
    private void renderIceElemental(PoseStack ps, MultiBufferSource buffer, int light, float gait, float age) {
        int frost = 0xFFE8FAFF;
        // legs are crystal columns — they do not swing, they glide
        propB(ps, buffer, BLUE_ICE, false, light, -2.0F, HIP_Y / 2.0F, 0.0F, 3.4F, HIP_Y, 3.4F, frost);
        propB(ps, buffer, BLUE_ICE, false, light, 2.0F, HIP_Y / 2.0F, 0.0F, 3.4F, HIP_Y, 3.4F, frost);
        torso(ps, buffer, BLUE_ICE, false, light, frost);
        arms(ps, buffer, ICE, false, light, gait * 0.7F, frost);
        head(ps, buffer, BLUE_ICE, false, light, frost, 4.4F);
        // shard crown orbiting the head
        int shards = 8;
        for (int i = 0; i < shards; i++) {
            double a = i * Math.PI * 2.0 / shards + age * 0.04F;
            double bob = Mth.sin(age * 0.09F + i * 1.3F) * 0.5F;
            ps.pushPose();
            ps.translate(Math.cos(a) * 3.2F, HEAD_Y + 2.2F + bob, Math.sin(a) * 3.2F);
            ps.mulPose(Axis.YP.rotation((float) -a));
            ps.mulPose(Axis.ZP.rotationDegrees(16.0F));
            VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(ICE));
            ps.scale(0.7F, 2.8F, 0.7F);
            cube.render(ps, vc, light, OverlayTexture.NO_OVERLAY, frost);
            ps.popPose();
        }
        // frozen core
        propB(ps, buffer, ICE, true, light, 0.0F, TORSO_Y + 0.7F, 2.0F, 1.8F, 1.8F, 0.9F, 0xFFBFEFFF);
    }

    // ---------------- EARTH ELEMENTAL (nature) — HoMM3 Conflux ----------------
    private void renderEarthElemental(PoseStack ps, MultiBufferSource buffer, int light, float gait, float age) {
        int moss = 0xFFE4FFD8;
        // legs of packed rooted dirt — heavy stompers
        leg(ps, buffer, ROOTED, false, light, -2.1F, -gait, moss);
        leg(ps, buffer, ROOTED, false, light, 2.1F, gait, moss);
        torso(ps, buffer, MOSSY, false, light, moss);
        arms(ps, buffer, MOSSY, false, light, gait * 0.8F, moss);
        head(ps, buffer, MOSSY, false, light, moss, 4.0F);
        // boulder shoulders
        propB(ps, buffer, MOSSY, false, light, -5.4F, SHOULDER_Y + 1.4F, 0.0F, 3.4F, 3.0F, 3.4F, moss);
        propB(ps, buffer, MOSSY, false, light, 5.4F, SHOULDER_Y + 1.4F, 0.0F, 3.4F, 3.0F, 3.4F, moss);
        // sprouting heart
        propB(ps, buffer, MOSSY, true, light, 0.0F, TORSO_Y + 0.7F, 2.0F, 1.6F, 1.6F, 0.8F, 0xFF7CFF6B);
    }

    // ---------------- giant body parts (all sizes in BLOCKS) ----------------
    private void torso(PoseStack ps, MultiBufferSource buffer, ResourceLocation tex, boolean emissive,
                       int light, int color) {
        propB(ps, buffer, tex, emissive, light, 0.0F, TORSO_Y, 0.0F, 8.0F, 8.0F, 4.0F, color);
    }

    private void head(PoseStack ps, MultiBufferSource buffer, ResourceLocation tex, boolean emissive,
                      int light, int color, float size) {
        propB(ps, buffer, tex, emissive, light, 0.0F, HEAD_Y, 0.0F, size, size, size, color);
    }

    private void arms(PoseStack ps, MultiBufferSource buffer, ResourceLocation tex, boolean emissive,
                      int light, float gait, int color) {
        for (int side = -1; side <= 1; side += 2) {
            ps.pushPose();
            ps.translate(side * 5.5F, SHOULDER_Y, 0.0F);
            ps.mulPose(Axis.XP.rotation(-gait * 0.7F * side));
            VertexConsumer vc = buffer.getBuffer(emissive
                    ? RenderType.entityTranslucent(tex) : RenderType.entityCutoutNoCull(tex));
            ps.translate(0.0F, -5.0F, 0.0F);
            ps.scale(3.0F, 10.0F, 3.0F);
            cube.render(ps, vc, light, OverlayTexture.NO_OVERLAY, color);
            ps.popPose();
        }
    }

    private void legs(PoseStack ps, MultiBufferSource buffer, ResourceLocation tex, boolean emissive,
                      int light, float gait, int color) {
        leg(ps, buffer, tex, emissive, light, -2.0F, -gait, color);
        leg(ps, buffer, tex, emissive, light, 2.0F, gait, color);
    }

    private void leg(PoseStack ps, MultiBufferSource buffer, ResourceLocation tex, boolean emissive,
                     int light, float x, float swing, int color) {
        ps.pushPose();
        ps.translate(x, HIP_Y, 0.0F);
        ps.mulPose(Axis.XP.rotation(swing * 0.55F));
        VertexConsumer vc = buffer.getBuffer(emissive
                ? RenderType.entityTranslucent(tex) : RenderType.entityCutoutNoCull(tex));
        ps.translate(0.0F, -5.5F, 0.0F);
        ps.scale(3.5F, 11.0F, 3.5F);
        cube.render(ps, vc, light, OverlayTexture.NO_OVERLAY, color);
        ps.popPose();
    }

    // ---------------- shared prop helpers (BLOCK units) ----------------
    // one textured cube: position = cube center, size in blocks
    private void propB(PoseStack ps, MultiBufferSource buffer, ResourceLocation tex, boolean emissive,
                       int light, float x, float y, float z, float sx, float sy, float sz, int color) {
        VertexConsumer vc = buffer.getBuffer(emissive
                ? RenderType.entityTranslucent(tex) : RenderType.entityCutoutNoCull(tex));
        ps.pushPose();
        ps.translate(x, y, z); // layer space is blocks (ModelPart divides its pixels by 16)
        ps.scale(sx, sy, sz);
        cube.render(ps, vc, light, OverlayTexture.NO_OVERLAY, color);
        ps.popPose();
    }

    // a ring of cubes on a tilted plane, spinning with age (block units)
    private void ringB(PoseStack ps, MultiBufferSource buffer, ResourceLocation tex, boolean emissive,
                       int light, float radius, int count, float cubeSize, float angle,
                       float tiltX, float tiltZ, float y, int color) {
        VertexConsumer vc = buffer.getBuffer(emissive
                ? RenderType.entityTranslucent(tex) : RenderType.entityCutoutNoCull(tex));
        ps.pushPose();
        ps.translate(0.0F, y, 0.0F); // blocks
        ps.mulPose(Axis.XP.rotation(tiltX));
        ps.mulPose(Axis.ZP.rotation(tiltZ));
        ps.mulPose(Axis.YP.rotation(angle));
        for (int i = 0; i < count; i++) {
            double a = i * Math.PI * 2.0 / count;
            ps.pushPose();
            ps.translate(Math.cos(a) * radius, 0.0F, Math.sin(a) * radius);
            ps.mulPose(Axis.YP.rotation((float) -a));
            ps.scale(cubeSize, cubeSize, cubeSize);
            cube.render(ps, vc, light, OverlayTexture.NO_OVERLAY, color);
            ps.popPose();
        }
        ps.popPose();
    }

    // the form breathes around the pilot: embers, bent light, snow, spores
    private void ambience(AbstractClientPlayer player, int elementId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || player.tickCount % 3 != 0) {
            return;
        }
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        double t = player.tickCount * 0.22;
        switch (elementId) {
            case 0 -> { // titan: spark drips off the rings
                if (player.tickCount % 8 != 0) {
                    return;
                }
                mc.level.addParticle(Element.LIGHTNING.spark(),
                        px + Math.cos(t) * 2.2, py + 6.0 + Math.random() * 8.0,
                        pz + Math.sin(t) * 2.2, 0.0, 0.02, 0.0);
            }
            case 1 -> { // efreet: embers fall from the vortex
                mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.LAVA,
                        px + (Math.random() - 0.5) * 3.0, py + Math.random() * 6.0,
                        pz + (Math.random() - 0.5) * 3.0, 0.0, 0.0, 0.0);
                mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,
                        px + (Math.random() - 0.5) * 2.5, py + Math.random() * 8.0,
                        pz + (Math.random() - 0.5) * 2.5, 0.0, 0.05, 0.0);
            }
            case 2 -> { // psychic elemental: light bends inward
                for (int i = 0; i < 2; i++) {
                    double a = t + i * Math.PI;
                    mc.level.addParticle(Element.VOID.spark(),
                            px + Math.cos(a) * 3.2, py + 6.0 + Math.random() * 8.0,
                            pz + Math.sin(a) * 3.2, -Math.cos(a) * 0.05, -0.02, -Math.sin(a) * 0.05);
                }
                mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                        px + (Math.random() - 0.5) * 1.6, py + Math.random() * 10.0,
                        pz + (Math.random() - 0.5) * 1.6, 0.0, 0.02, 0.0);
            }
            case 3 -> { // ice elemental: snow whips around the columns
                for (int i = 0; i < 3; i++) {
                    double a = t * 1.4 + i * 2.1;
                    mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.SNOWFLAKE,
                            px + Math.cos(a) * 2.6, py + 0.2 + Math.random() * 10.0,
                            pz + Math.sin(a) * 2.6, -Math.sin(a) * 0.05, 0.01, Math.cos(a) * 0.05);
                }
            }
            case 4 -> { // earth elemental: spores and life motes
                mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                        px + (Math.random() - 0.5) * 3.0, py + 2.0 + Math.random() * 12.0,
                        pz + (Math.random() - 0.5) * 3.0, 0.0, 0.02, 0.0);
                mc.level.addParticle(Element.NATURE.spark(),
                        px + (Math.random() - 0.5) * 2.4, py + Math.random() * 8.0,
                        pz + (Math.random() - 0.5) * 2.4, 0.0, 0.03, 0.0);
            }
            default -> {
            }
        }
    }
}
