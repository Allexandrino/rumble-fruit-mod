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

// the titan forms, redesigned from scratch — no wings, no robes, no halos:
//   lightning: STORM DJINN — three gyroscopic rings spin around the torso,
//              a storm vortex whips where the legs should be
//   inferno:   MOLTEN COLOSSUS — huge magma plate armor, the biggest body
//   void:      LIVING SINGULARITY — a debris ring of obsidian orbiting the
//              chest, an amethyst heart pulsing inside it
//   frost:     BLIZZARD SPIRIT — a hovering crown of ice shards orbiting
//              the head, snow whipping at the feet
//   nature:    MYCELIUM SOVEREIGN — a giant mushroom cap, spore motes,
//              the ground turns to mycelium underfoot
// everything is built from ONE 16px unit cube (full-texture faces) that gets
// transformed per prop; textures are vanilla blocks, tinted per element.
public class TitanPropsLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "titan_props"), "main");

    private static final ResourceLocation SEA_LANTERN = vanilla("textures/block/sea_lantern.png");
    private static final ResourceLocation MAGMA = vanilla("textures/block/magma.png");
    private static final ResourceLocation OBSIDIAN = vanilla("textures/block/obsidian.png");
    private static final ResourceLocation AMETHYST = vanilla("textures/block/amethyst_block.png");
    private static final ResourceLocation BLUE_ICE = vanilla("textures/block/blue_ice.png");
    private static final ResourceLocation MUSHROOM_RED = vanilla("textures/block/red_mushroom_block.png");
    private static final ResourceLocation MUSHROOM_STEM = vanilla("textures/block/mushroom_stem.png");

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
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!ClientWingsData.isActive(player.getUUID())) {
            return;
        }
        float unfold = Math.max(0.05F, ClientWingsData.unfoldProgress(player.getUUID(), ageInTicks));
        int elementId = com.rumblefruit.core.ElementCatalog.byId(ClientPowerData.element()).id();
        switch (elementId) {
            case 0 -> renderDjinn(poseStack, buffer, packedLight, ageInTicks, unfold);
            case 1 -> renderColossus(poseStack, buffer, packedLight, ageInTicks, unfold);
            case 2 -> renderSingularity(poseStack, buffer, packedLight, ageInTicks, unfold);
            case 3 -> renderSpirit(poseStack, buffer, packedLight, ageInTicks, unfold);
            case 4 -> renderMycelium(poseStack, buffer, packedLight, ageInTicks, unfold);
            default -> {
            }
        }
        ambience(player, elementId);
    }

    // ---------------- storm djinn (lightning) ----------------
    private void renderDjinn(PoseStack ps, MultiBufferSource buffer, int light, float age, float unfold) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(SEA_LANTERN));
        ps.pushPose();
        this.getParentModel().body.translateAndRotate(ps);
        // three gyroscopic rings, each on its own axis and spin speed
        ring(ps, vc, light, 15.0F, 16, 2.2F, age * 0.045F, 0.0F, 0.0F, 6.0F, unfold);
        ring(ps, vc, light, 18.0F, 16, 1.9F, -age * 0.032F, 0.9F, 0.0F, 6.0F, unfold);
        ring(ps, vc, light, 21.0F, 20, 1.6F, age * 0.021F, -0.6F, 0.6F, 6.0F, unfold);
        ps.popPose();
    }

    // ---------------- molten colossus (inferno) ----------------
    private void renderColossus(PoseStack ps, MultiBufferSource buffer, int light, float age, float unfold) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(MAGMA));
        ps.pushPose();
        this.getParentModel().body.translateAndRotate(ps);
        // chest plate
        prop(ps, vc, light, 0.0F, 6.0F, 0.0F, 10.5F, 12.5F, 6.0F, unfold);
        // pauldrons: huge slabs over the shoulders
        prop(ps, vc, light, -7.6F, 0.5F, 0.0F, 6.5F, 4.5F, 7.5F, unfold);
        prop(ps, vc, light, 7.6F, 0.5F, 0.0F, 6.5F, 4.5F, 7.5F, unfold);
        // belt plate
        prop(ps, vc, light, 0.0F, 12.6F, 0.0F, 9.5F, 2.4F, 5.2F, unfold);
        ps.popPose();
        ps.pushPose();
        this.getParentModel().rightArm.translateAndRotate(ps);
        prop(ps, vc, light, -0.2F, 6.4F, 0.0F, 5.4F, 6.0F, 5.4F, unfold); // gauntlet
        ps.popPose();
        ps.pushPose();
        this.getParentModel().leftArm.translateAndRotate(ps);
        prop(ps, vc, light, 0.2F, 6.4F, 0.0F, 5.4F, 6.0F, 5.4F, unfold); // gauntlet
        ps.popPose();
        ps.pushPose();
        this.getParentModel().head.translateAndRotate(ps);
        prop(ps, vc, light, 0.0F, -2.6F, 0.0F, 9.4F, 3.0F, 9.4F, unfold); // visor brow
        ps.popPose();
    }

    // ---------------- living singularity (void) ----------------
    private void renderSingularity(PoseStack ps, MultiBufferSource buffer, int light, float age, float unfold) {
        VertexConsumer obsidian = buffer.getBuffer(RenderType.entityCutoutNoCull(OBSIDIAN));
        VertexConsumer heart = buffer.getBuffer(RenderType.entityCutoutNoCull(AMETHYST));
        ps.pushPose();
        this.getParentModel().body.translateAndRotate(ps);
        // debris ring, horizontal, slow
        ring(ps, obsidian, light, 16.0F, 8, 2.8F, age * 0.018F, 0.0F, 0.0F, 5.0F, unfold);
        // counter-ring on a steep tilt
        ring(ps, obsidian, light, 19.0F, 6, 2.2F, -age * 0.027F, 0.62F, 0.0F, 5.0F, unfold);
        // the amethyst heart, pulsing inside the rings
        float pulse = 3.2F + (float) Math.sin(age * 0.13F) * 0.9F;
        prop(ps, heart, light, 0.0F, 4.0F, 0.0F, pulse, pulse, pulse, unfold);
        ps.popPose();
    }

    // ---------------- blizzard spirit (frost) ----------------
    private void renderSpirit(PoseStack ps, MultiBufferSource buffer, int light, float age, float unfold) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(BLUE_ICE));
        ps.pushPose();
        this.getParentModel().head.translateAndRotate(ps);
        // hovering crown: six shards orbit the head, each bobbing on its own phase
        int shards = 6;
        for (int i = 0; i < shards; i++) {
            double a = i * Math.PI * 2.0 / shards + age * 0.05F;
            double bob = Math.sin(age * 0.11F + i * 1.7F) * 1.6F;
            ps.pushPose();
            ps.translate(Math.cos(a) * 9.5F / 16.0F, (-6.5F + bob) / 16.0F, Math.sin(a) * 9.5F / 16.0F);
            ps.mulPose(Axis.YP.rotation((float) -a));
            ps.mulPose(Axis.ZP.rotationDegrees(14.0F));
            ps.scale((2.0F / 16.0F) * unfold, (6.5F / 16.0F) * unfold, (2.0F / 16.0F) * unfold);
            cube.render(ps, vc, light, OverlayTexture.NO_OVERLAY);
            ps.popPose();
        }
        ps.popPose();
    }

    // ---------------- mycelium sovereign (nature) ----------------
    private void renderMycelium(PoseStack ps, MultiBufferSource buffer, int light, float age, float unfold) {
        VertexConsumer cap = buffer.getBuffer(RenderType.entityCutoutNoCull(MUSHROOM_RED));
        VertexConsumer stem = buffer.getBuffer(RenderType.entityCutoutNoCull(MUSHROOM_STEM));
        ps.pushPose();
        this.getParentModel().head.translateAndRotate(ps);
        // the giant cap: red dome + stem ring under it, swaying like a real toadstool
        ps.mulPose(Axis.ZP.rotationDegrees((float) Math.sin(age * 0.05F) * 4.0F));
        prop(ps, cap, light, 0.0F, -6.2F, 0.0F, 13.0F, 3.6F, 13.0F, unfold);
        prop(ps, stem, light, 0.0F, -4.6F, 0.0F, 9.0F, 1.8F, 9.0F, unfold);
        ps.popPose();
        ps.pushPose();
        this.getParentModel().body.translateAndRotate(ps);
        // two little toadstools on the shoulders
        prop(ps, stem, light, -6.0F, -0.6F, 0.0F, 1.6F, 3.0F, 1.6F, unfold);
        prop(ps, cap, light, -6.0F, -2.6F, 0.0F, 3.4F, 1.6F, 3.4F, unfold);
        prop(ps, stem, light, 6.0F, -0.4F, 0.0F, 1.4F, 2.4F, 1.4F, unfold);
        prop(ps, cap, light, 6.0F, -2.0F, 0.0F, 2.8F, 1.4F, 2.8F, unfold);
        ps.popPose();
    }

    // ---------------- shared prop helpers ----------------
    // one textured cube: position and size in player-model pixels (16px = 1 block)
    private void prop(PoseStack ps, VertexConsumer vc, int light,
                      float x, float y, float z, float sx, float sy, float sz, float unfold) {
        ps.pushPose();
        ps.translate(x / 16.0F, y / 16.0F, z / 16.0F);
        ps.scale((sx / 16.0F) * unfold, (sy / 16.0F) * unfold, (sz / 16.0F) * unfold);
        cube.render(ps, vc, light, OverlayTexture.NO_OVERLAY);
        ps.popPose();
    }

    // a ring of small cubes on a tilted plane, spinning with age
    private void ring(PoseStack ps, VertexConsumer vc, int light,
                      float radiusPx, int count, float cubePx, float angle,
                      float tiltX, float tiltZ, float yPx, float unfold) {
        ps.pushPose();
        ps.translate(0.0F, yPx / 16.0F, 0.0F);
        ps.mulPose(Axis.XP.rotation(tiltX));
        ps.mulPose(Axis.ZP.rotation(tiltZ));
        ps.mulPose(Axis.YP.rotation(angle));
        for (int i = 0; i < count; i++) {
            double a = i * Math.PI * 2.0 / count;
            ps.pushPose();
            ps.translate(Math.cos(a) * radiusPx / 16.0F, 0.0F, Math.sin(a) * radiusPx / 16.0F);
            ps.mulPose(Axis.YP.rotation((float) -a));
            ps.scale(cubePx / 16.0F * unfold, cubePx / 16.0F * unfold, cubePx / 16.0F * unfold);
            cube.render(ps, vc, light, OverlayTexture.NO_OVERLAY);
            ps.popPose();
        }
        ps.popPose();
    }

    // the form breathes: djinn vortex, colossus embers, singularity smoke,
    // spirit snow, sovereign spores — client-side ambience, tinted per form
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
            case 0 -> { // storm vortex swirling at the djinn's base
                for (int i = 0; i < 2; i++) {
                    double a = t + i * Math.PI;
                    mc.level.addParticle(com.rumblefruit.ModParticles.ELECTRO_CLOUD.get(),
                            px + Math.cos(a) * 0.55, py + 0.15 + Math.random() * 0.4,
                            pz + Math.sin(a) * 0.55, 0.0, 0.05, 0.0);
                }
            }
            case 1 -> { // embers drip off the colossus
                mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.LAVA,
                        px + (Math.random() - 0.5) * 1.0, py + Math.random() * 2.2,
                        pz + (Math.random() - 0.5) * 1.0, 0.0, 0.0, 0.0);
                mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,
                        px + (Math.random() - 0.5) * 0.9, py + Math.random() * 2.0,
                        pz + (Math.random() - 0.5) * 0.9, 0.0, 0.05, 0.0);
            }
            case 2 -> { // the singularity leaks bent light
                for (int i = 0; i < 2; i++) {
                    double a = t + i * Math.PI;
                    mc.level.addParticle(Element.VOID.spark(),
                            px + Math.cos(a) * 1.1, py + 0.8 + Math.random(),
                            pz + Math.sin(a) * 1.1, -Math.cos(a) * 0.04, -0.02, -Math.sin(a) * 0.04);
                }
                mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                        px + (Math.random() - 0.5) * 0.6, py + Math.random() * 1.5,
                        pz + (Math.random() - 0.5) * 0.6, 0.0, 0.02, 0.0);
            }
            case 3 -> { // snow whips around the spirit
                for (int i = 0; i < 3; i++) {
                    double a = t * 1.4 + i * 2.1;
                    mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.SNOWFLAKE,
                            px + Math.cos(a) * 0.9, py + 0.1 + Math.random() * 1.6,
                            pz + Math.sin(a) * 0.9, -Math.sin(a) * 0.05, 0.01, Math.cos(a) * 0.05);
                }
            }
            case 4 -> { // spore motes drift off the cap
                mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                        px + (Math.random() - 0.5) * 1.0, py + 1.4 + Math.random() * 1.2,
                        pz + (Math.random() - 0.5) * 1.0, 0.0, 0.02, 0.0);
                mc.level.addParticle(Element.NATURE.spark(),
                        px + (Math.random() - 0.5) * 0.8, py + Math.random() * 1.8,
                        pz + (Math.random() - 0.5) * 0.8, 0.0, 0.03, 0.0);
            }
            default -> {
            }
        }
    }
}
