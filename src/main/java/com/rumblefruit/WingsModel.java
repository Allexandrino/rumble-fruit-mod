package com.rumblefruit;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

// angel wings: two white feathered wings (covert + primary feather layers) on the back,
// plus the hazbin-hotel style exorcist mask (horned) rendered in head space.
public class WingsModel extends Model {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "wings"), "main");

    private final ModelPart bodyRoot;
    private final ModelPart rightWing;
    private final ModelPart leftWing;
    private final ModelPart[] styleRoots = new ModelPart[5]; // 0 = angel body
    private final ModelPart mask;
    private final ModelPart halo;
    private final ModelPart horns;
    private final ModelPart crown;
    private final ModelPart circlet;
    private final ModelPart costume;

    public WingsModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.bodyRoot = root.getChild("body");
        this.rightWing = bodyRoot.getChild("right_wing");
        this.leftWing = bodyRoot.getChild("left_wing");
        styleRoots[0] = bodyRoot;
        for (int s = 1; s <= 4; s++) {
            styleRoots[s] = root.getChild("wings" + s);
        }
        this.mask = root.getChild("mask");
        this.halo = root.getChild("halo");
        this.horns = root.getChild("horns");
        this.crown = root.getChild("crown");
        this.circlet = root.getChild("circlet");
        this.costume = root.getChild("costume");
    }

    // one angel wing: long primary feathers fanning from up-out to down-out,
    // a second row of secondary feathers behind them, shorter coverts on top
    private static void buildWing(PartDefinition wing, boolean mirrored) {
        // primaries: pivot along the shoulder arc, feather strip fanning downward
        float[][] primaries = {
                // px, py, zRot, length
                {0.8F, -0.8F, -0.90F, 7.0F},
                {2.0F, -1.6F, -0.55F, 8.0F},
                {2.9F, -2.5F, -0.20F, 9.0F},
                {3.4F, -3.3F, 0.15F, 9.0F},
                {3.5F, -4.0F, 0.50F, 8.0F},
                {3.4F, -4.5F, 0.85F, 7.0F},
        };
        for (int i = 0; i < primaries.length; i++) {
            float px = mirrored ? -primaries[i][0] : primaries[i][0];
            float zRot = mirrored ? -primaries[i][2] : primaries[i][2];
            float len = primaries[i][3];
            CubeListBuilder cube = CubeListBuilder.create().texOffs(0, 4);
            if (mirrored) {
                cube.addBox(-len, -1.3F, -0.75F, len, 2.6F, 1.1F);
            } else {
                cube.addBox(0.0F, -1.3F, -0.75F, len, 2.6F, 1.1F);
            }
            wing.addOrReplaceChild("p" + i, cube,
                    PartPose.offsetAndRotation(px, primaries[i][1], 0.0F, 0.0F, 0.0F, zRot));
        }
        // secondaries: a longer, fuller row behind the primaries (more wingspan + detail)
        float[][] secondaries = {
                {0.4F, -0.2F, -0.75F, 9.0F},
                {1.4F, -0.9F, -0.40F, 10.0F},
                {2.2F, -1.6F, -0.05F, 11.0F},
                {2.6F, -2.3F, 0.30F, 10.0F},
        };
        for (int i = 0; i < secondaries.length; i++) {
            float px = mirrored ? -secondaries[i][0] : secondaries[i][0];
            float zRot = mirrored ? -secondaries[i][2] : secondaries[i][2];
            float len = secondaries[i][3];
            CubeListBuilder cube = CubeListBuilder.create().texOffs(0, 8);
            if (mirrored) {
                cube.addBox(-len, -1.4F, 0.15F, len, 2.9F, 1.2F);
            } else {
                cube.addBox(0.0F, -1.4F, 0.15F, len, 2.9F, 1.2F);
            }
            wing.addOrReplaceChild("s" + i, cube,
                    PartPose.offsetAndRotation(px, secondaries[i][1], 0.15F, 0.0F, 0.0F, zRot));
        }
        // coverts: shorter layer, slightly toward the back
        float[][] coverts = {
                {0.5F, -0.5F, -0.70F, 4.0F},
                {1.5F, -1.1F, -0.35F, 4.5F},
                {2.2F, -1.8F, 0.00F, 5.0F},
                {2.6F, -2.4F, 0.35F, 4.5F},
        };
        for (int i = 0; i < coverts.length; i++) {
            float px = mirrored ? -coverts[i][0] : coverts[i][0];
            float zRot = mirrored ? -coverts[i][2] : coverts[i][2];
            float len = coverts[i][3];
            CubeListBuilder cube = CubeListBuilder.create().texOffs(0, 0);
            if (mirrored) {
                cube.addBox(-len, -1.15F, -1.25F, len, 2.3F, 0.9F);
            } else {
                cube.addBox(0.0F, -1.15F, -1.25F, len, 2.3F, 0.9F);
            }
            wing.addOrReplaceChild("c" + i, cube,
                    PartPose.offsetAndRotation(px, coverts[i][1], 0.3F, 0.0F, 0.0F, zRot));
        }
    }

    // flame tongues: slim vertical flames licking upward (inferno archangel)
    private static void buildFlameWing(PartDefinition wing, boolean mirrored) {
        float[][] t = {
                {0.8F, -0.4F, 0.95F, 5.0F},
                {1.8F, -0.9F, 0.60F, 6.5F},
                {2.6F, -1.5F, 0.25F, 8.0F},
                {3.2F, -2.1F, -0.10F, 8.5F},
                {3.6F, -2.6F, -0.45F, 7.0F},
        };
        for (int i = 0; i < t.length; i++) {
            float px = mirrored ? -t[i][0] : t[i][0];
            float zRot = mirrored ? -t[i][2] : t[i][2];
            float len = t[i][3];
            wing.addOrReplaceChild("f" + i,
                    CubeListBuilder.create().texOffs(0, 4).addBox(-0.65F, -len, -0.55F, 1.3F, len, 1.1F),
                    PartPose.offsetAndRotation(px, t[i][1], 0.0F, 0.0F, 0.0F, zRot));
        }
    }

    // bat wing: three bone fingers with a membrane stretched between (void king)
    private static void buildBatWing(PartDefinition wing, boolean mirrored) {
        float[][] fingers = {
                {0.8F, -0.8F, -0.75F, 8.0F},
                {1.2F, -1.6F, -0.25F, 9.5F},
                {1.2F, -2.4F, 0.35F, 8.5F},
        };
        for (int i = 0; i < fingers.length; i++) {
            float px = mirrored ? -fingers[i][0] : fingers[i][0];
            float zRot = mirrored ? -fingers[i][2] : fingers[i][2];
            float len = fingers[i][3];
            CubeListBuilder cube = CubeListBuilder.create().texOffs(0, 8);
            if (mirrored) {
                cube.addBox(-len, -0.5F, -0.4F, len, 1.0F, 0.9F);
            } else {
                cube.addBox(0.0F, -0.5F, -0.4F, len, 1.0F, 0.9F);
            }
            wing.addOrReplaceChild("bf" + i, cube,
                    PartPose.offsetAndRotation(px, fingers[i][1], 0.0F, 0.0F, 0.0F, zRot));
        }
        float[][] mem = {
                {0.6F, -0.6F, -0.55F, 7.0F},
                {1.0F, -1.4F, -0.05F, 8.5F},
        };
        for (int i = 0; i < mem.length; i++) {
            float px = mirrored ? -mem[i][0] : mem[i][0];
            float zRot = mirrored ? -mem[i][2] : mem[i][2];
            float len = mem[i][3];
            CubeListBuilder cube = CubeListBuilder.create().texOffs(0, 12);
            if (mirrored) {
                cube.addBox(-len, -2.1F, 0.15F, len, 4.2F, 0.7F);
            } else {
                cube.addBox(0.0F, -2.1F, 0.15F, len, 4.2F, 0.7F);
            }
            wing.addOrReplaceChild("bm" + i, cube,
                    PartPose.offsetAndRotation(px, mem[i][1], 0.1F, 0.0F, 0.0F, zRot));
        }
    }

    // crystal shards: slim icy prisms fanned upward, every shard crowned with
    // a watching eye (ice seraphim — many-eyed by design)
    private static void buildCrystalWing(PartDefinition wing, boolean mirrored) {
        float[][] shards = {
                {0.7F, -0.6F, 0.70F, 5.5F},
                {1.6F, -1.2F, 0.40F, 7.0F},
                {2.4F, -1.8F, 0.10F, 8.5F},
                {3.0F, -2.4F, -0.20F, 8.0F},
                {3.4F, -2.9F, -0.50F, 6.5F},
        };
        for (int i = 0; i < shards.length; i++) {
            float px = mirrored ? -shards[i][0] : shards[i][0];
            float zRot = mirrored ? -shards[i][2] : shards[i][2];
            float len = shards[i][3];
            wing.addOrReplaceChild("cr" + i,
                    CubeListBuilder.create().texOffs(0, 4).addBox(-0.7F, -len, -0.6F, 1.4F, len, 1.2F),
                    PartPose.offsetAndRotation(px, shards[i][1], 0.0F, 0.0F, 0.0F, zRot));
            // an eye at the shard's tip: white sclera + dark pupil staring out
            wing.addOrReplaceChild("eye_w" + i,
                    CubeListBuilder.create().texOffs(40, 0).addBox(-0.7F, -len - 0.7F, -0.5F, 1.4F, 1.4F, 0.9F),
                    PartPose.offsetAndRotation(px, shards[i][1], 0.0F, 0.0F, 0.0F, zRot));
            wing.addOrReplaceChild("eye_p" + i,
                    CubeListBuilder.create().texOffs(40, 8).addBox(-0.35F, -len - 0.35F, -0.65F, 0.7F, 0.7F, 0.35F),
                    PartPose.offsetAndRotation(px, shards[i][1], 0.0F, 0.0F, 0.0F, zRot));
        }
    }

    // leaf fan: broad leaves layered in a row (mother nature)
    private static void buildLeafWing(PartDefinition wing, boolean mirrored) {
        float[][] leaves = {
                {0.8F, -0.7F, -0.80F, 5.5F},
                {1.9F, -1.4F, -0.45F, 6.5F},
                {2.7F, -2.1F, -0.10F, 7.0F},
                {3.2F, -2.7F, 0.30F, 6.0F},
        };
        for (int i = 0; i < leaves.length; i++) {
            float px = mirrored ? -leaves[i][0] : leaves[i][0];
            float zRot = mirrored ? -leaves[i][2] : leaves[i][2];
            float len = leaves[i][3];
            CubeListBuilder cube = CubeListBuilder.create().texOffs(0, 0);
            if (mirrored) {
                cube.addBox(-len, -1.8F, -0.6F, len, 3.6F, 1.0F);
            } else {
                cube.addBox(0.0F, -1.8F, -0.6F, len, 3.6F, 1.0F);
            }
            wing.addOrReplaceChild("lf" + i, cube,
                    PartPose.offsetAndRotation(px, leaves[i][1], 0.0F, 0.0F, 0.0F, zRot));
        }
    }

    private static void buildStylePair(PartDefinition body, int style, boolean ignored) {
        PartDefinition pair = body.addOrReplaceChild("wings" + style,
                CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition rw = pair.addOrReplaceChild("rw",
                CubeListBuilder.create(), PartPose.offset(1.4F, 0.4F, 3.0F));
        buildStyledWing(rw, false, style);
        PartDefinition lw = pair.addOrReplaceChild("lw",
                CubeListBuilder.create(), PartPose.offset(-1.4F, 0.4F, 3.0F));
        buildStyledWing(lw, true, style);
    }

    private static void buildStyledWing(PartDefinition wing, boolean mirrored, int style) {
        switch (style) {
            case 1 -> buildFlameWing(wing, mirrored);
            case 2 -> buildBatWing(wing, mirrored);
            case 3 -> buildCrystalWing(wing, mirrored);
            default -> buildLeafWing(wing, mirrored);
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);

        PartDefinition right = body.addOrReplaceChild("right_wing",
                CubeListBuilder.create(), PartPose.offset(1.4F, 0.4F, 3.0F));
        buildWing(right, false);
        PartDefinition left = body.addOrReplaceChild("left_wing",
                CubeListBuilder.create(), PartPose.offset(-1.4F, 0.4F, 3.0F));
        buildWing(left, true);
        // elemental wing styles: flame, bat, crystal, leaf — each its own root
        // (same render path as the classic angel body root)
        buildStylePair(root, 1, false);
        buildStylePair(root, 2, false);
        buildStylePair(root, 3, false);
        buildStylePair(root, 4, false);
        // the seraphim is many-winged: a second crystal fan rides behind the first
        PartDefinition wings3 = root.getChild("wings3");
        PartDefinition rw2 = wings3.addOrReplaceChild("rw2",
                CubeListBuilder.create(), PartPose.offset(1.2F, 1.2F, 3.8F));
        buildCrystalWing(rw2, false);
        PartDefinition lw2 = wings3.addOrReplaceChild("lw2",
                CubeListBuilder.create(), PartPose.offset(-1.2F, 1.2F, 3.8F));
        buildCrystalWing(lw2, true);

        // hazbin-hotel exorcist mask: white horned mask over the face (head space)
        PartDefinition mask = root.addOrReplaceChild("mask",
                CubeListBuilder.create().texOffs(0, 20).addBox(-4.5F, -8.5F, -5.2F, 9.0F, 9.0F, 1.0F),
                PartPose.ZERO);
        mask.addOrReplaceChild("horn_r",
                CubeListBuilder.create().texOffs(32, 0).addBox(-0.9F, -4.6F, -0.9F, 1.8F, 4.6F, 1.8F),
                PartPose.offsetAndRotation(3.4F, -8.0F, -1.0F, 0.0F, 0.0F, -0.5F));
        mask.addOrReplaceChild("horn_l",
                CubeListBuilder.create().texOffs(32, 0).addBox(-0.9F, -4.6F, -0.9F, 1.8F, 4.6F, 1.8F),
                PartPose.offsetAndRotation(-3.4F, -8.0F, -1.0F, 0.0F, 0.0F, 0.5F));

        // golden halo floating above the head (head space): thin square ring with god spikes
        PartDefinition halo = root.addOrReplaceChild("halo", CubeListBuilder.create(), PartPose.offset(0.0F, -11.5F, 0.0F));
        halo.addOrReplaceChild("h_front",
                CubeListBuilder.create().texOffs(32, 16).addBox(-4.0F, -0.25F, -4.6F, 8.0F, 0.5F, 1.2F), PartPose.ZERO);
        halo.addOrReplaceChild("h_back",
                CubeListBuilder.create().texOffs(32, 16).addBox(-4.0F, -0.25F, 3.4F, 8.0F, 0.5F, 1.2F), PartPose.ZERO);
        halo.addOrReplaceChild("h_left",
                CubeListBuilder.create().texOffs(32, 18).addBox(-4.6F, -0.25F, -3.4F, 1.2F, 0.5F, 6.8F), PartPose.ZERO);
        halo.addOrReplaceChild("h_right",
                CubeListBuilder.create().texOffs(32, 18).addBox(3.4F, -0.25F, -3.4F, 1.2F, 0.5F, 6.8F), PartPose.ZERO);
        // sharp god rays on the halo corners (alastor-style drama)
        float[][] spikes = {{-4.0F, -4.0F}, {4.0F, -4.0F}, {-4.0F, 4.0F}, {4.0F, 4.0F}};
        for (int i = 0; i < spikes.length; i++) {
            halo.addOrReplaceChild("spike" + i,
                    CubeListBuilder.create().texOffs(32, 24).addBox(-0.5F, -2.6F, -0.5F, 1.0F, 2.6F, 1.0F),
                    PartPose.offset(spikes[i][0], 0.0F, spikes[i][1]));
        }

        // electro-god costume extras (body space): high collar + sharp coat tails
        // (pushed back so they clear the inflated robe shell)
        PartDefinition costume = root.addOrReplaceChild("costume", CubeListBuilder.create(), PartPose.ZERO);
        costume.addOrReplaceChild("collar",
                CubeListBuilder.create().texOffs(0, 32).addBox(-4.5F, -1.0F, 0.0F, 9.0F, 4.5F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 0.4F, 2.9F, 0.42F, 0.0F, 0.0F));
        costume.addOrReplaceChild("coat_l",
                CubeListBuilder.create().texOffs(16, 32).addBox(-4.2F, 0.0F, 0.0F, 4.0F, 9.0F, 0.6F),
                PartPose.offsetAndRotation(0.0F, 11.5F, 2.9F, 0.14F, 0.0F, 0.06F));
        costume.addOrReplaceChild("coat_r",
                CubeListBuilder.create().texOffs(24, 32).addBox(0.2F, 0.0F, 0.0F, 4.0F, 9.0F, 0.6F),
                PartPose.offsetAndRotation(0.0F, 11.5F, 2.9F, 0.14F, 0.0F, -0.06F));

        // DEMON HORNS (inferno): big curved horns sweeping up from the temples
        PartDefinition horns = root.addOrReplaceChild("horns", CubeListBuilder.create(), PartPose.ZERO);
        for (int side = -1; side <= 1; side += 2) {
            horns.addOrReplaceChild("horn_base" + side,
                    CubeListBuilder.create().texOffs(32, 0).addBox(-0.9F, -3.2F, -0.9F, 1.8F, 3.2F, 1.8F),
                    PartPose.offsetAndRotation(4.2F * side, -6.2F, -0.6F, -0.35F, 0.0F, 0.45F * side));
            horns.addOrReplaceChild("horn_tip" + side,
                    CubeListBuilder.create().texOffs(32, 0).addBox(-0.6F, -2.8F, -0.6F, 1.2F, 2.8F, 1.2F),
                    PartPose.offsetAndRotation(5.6F * side, -8.6F, -1.4F, -0.7F, 0.0F, 0.75F * side));
        }
        // CROWN OF DARKNESS (void): jagged spiked crown around the head
        PartDefinition crown = root.addOrReplaceChild("crown", CubeListBuilder.create(), PartPose.ZERO);
        float[][] crownSpikes = {{-3.6F, -3.6F}, {3.6F, -3.6F}, {-3.6F, 3.6F}, {3.6F, 3.6F},
                {0.0F, -4.2F}, {0.0F, 4.2F}, {-4.2F, 0.0F}, {4.2F, 0.0F}};
        for (int i = 0; i < crownSpikes.length; i++) {
            crown.addOrReplaceChild("spike" + i,
                    CubeListBuilder.create().texOffs(32, 24).addBox(-0.55F, -3.4F, -0.55F, 1.1F, 3.4F, 1.1F),
                    PartPose.offset(crownSpikes[i][0], -7.2F, crownSpikes[i][1]));
        }
        crown.addOrReplaceChild("band_front",
                CubeListBuilder.create().texOffs(32, 16).addBox(-4.2F, -1.0F, -4.2F, 8.4F, 1.4F, 1.0F),
                PartPose.offset(0.0F, -7.0F, 0.0F));
        crown.addOrReplaceChild("band_back",
                CubeListBuilder.create().texOffs(32, 16).addBox(-4.2F, -1.0F, 3.2F, 8.4F, 1.4F, 1.0F),
                PartPose.offset(0.0F, -7.0F, 0.0F));
        crown.addOrReplaceChild("band_left",
                CubeListBuilder.create().texOffs(32, 18).addBox(-4.2F, -1.0F, -3.2F, 1.0F, 1.4F, 6.4F),
                PartPose.offset(0.0F, -7.0F, 0.0F));
        crown.addOrReplaceChild("band_right",
                CubeListBuilder.create().texOffs(32, 18).addBox(3.2F, -1.0F, -3.2F, 1.0F, 1.4F, 6.4F),
                PartPose.offset(0.0F, -7.0F, 0.0F));
        // FLOWER CIRCLET (nature): a ring of little leaf-flowers around the head
        PartDefinition circlet = root.addOrReplaceChild("circlet", CubeListBuilder.create(), PartPose.ZERO);
        for (int i = 0; i < 10; i++) {
            double a = i * Math.PI / 5.0;
            circlet.addOrReplaceChild("flower" + i,
                    CubeListBuilder.create().texOffs(0, 0).addBox(-0.8F, -0.8F, -0.8F, 1.6F, 1.6F, 1.6F),
                    PartPose.offset((float) (Math.cos(a) * 4.4), -7.6F, (float) (Math.sin(a) * 4.4)));
        }

        return LayerDefinition.create(mesh, 64, 64);
    }

    // flap the wings by flight mode: idle hover = wide spread + strong slow
    // beats; fast flight = swept back; ground = folded drape; plummeting =
    // wings stream UP like a parachute
    public void setFlap(float ageInTicks, boolean flying, boolean fastFlight, float fallSpeed) {
        float speed;
        float amp;
        float base;
        float liftBase;
        if (!flying) {
            speed = 0.1F; amp = 0.05F; base = 0.55F; liftBase = 0.04F;
        } else if (fastFlight) {
            speed = 0.45F; amp = 0.45F; base = 0.30F; liftBase = 0.04F; // swept for speed
        } else {
            speed = 0.18F; amp = 0.30F; base = 0.75F; liftBase = 0.06F; // hover stance: WIDE
        }
        float fold = Mth.sin(ageInTicks * speed) * amp;
        float lift = Mth.cos(ageInTicks * speed) * amp * 0.6F;
        float trail = Mth.clamp(fallSpeed * 0.15F, 0.0F, 0.35F); // fall physics, subtle
        flapPair(rightWing, leftWing, base, fold, liftBase, lift, trail);
        for (int s = 1; s <= 4; s++) {
            flapPair(styleRoots[s].getChild("rw"), styleRoots[s].getChild("lw"),
                    base, fold, liftBase, lift, trail);
        }
    }

    private static void flapPair(ModelPart right, ModelPart left,
                                 float base, float fold, float liftBase, float lift, float trail) {
        right.yRot = -base - fold;
        left.yRot = base + fold;
        right.zRot = -(liftBase + lift + trail);
        left.zRot = liftBase + lift + trail;
    }

    public void renderWings(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay) {
        renderWings(0, poseStack, buffer, packedLight, packedOverlay);
    }

    public void renderWings(int style, PoseStack poseStack, VertexConsumer buffer,
                            int packedLight, int packedOverlay) {
        styleRoots[Math.floorMod(style, 5)].render(poseStack, buffer, packedLight, packedOverlay);
    }

    public void renderWings(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                            int color) {
        renderWings(0, poseStack, buffer, packedLight, packedOverlay, color);
    }

    public void renderWings(int style, PoseStack poseStack, VertexConsumer buffer,
                            int packedLight, int packedOverlay, int color) {
        styleRoots[Math.floorMod(style, 5)].render(poseStack, buffer, packedLight, packedOverlay, color);
    }

    public void renderHalo(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                           int color) {
        halo.render(poseStack, buffer, packedLight, packedOverlay, color);
    }

    // themed headgear: demon horns / crown of darkness / flower circlet
    public void renderHorns(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                            int color) {
        horns.render(poseStack, buffer, packedLight, packedOverlay, color);
    }

    public void renderCrown(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                            int color) {
        crown.render(poseStack, buffer, packedLight, packedOverlay, color);
    }

    public void renderCirclet(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                              int color) {
        circlet.render(poseStack, buffer, packedLight, packedOverlay, color);
    }

    public void renderCostume(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                              int color) {
        costume.render(poseStack, buffer, packedLight, packedOverlay, color);
    }

    public void renderMask(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay) {
        mask.render(poseStack, buffer, packedLight, packedOverlay);
    }

    public void renderHalo(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay) {
        halo.render(poseStack, buffer, packedLight, packedOverlay);
    }

    public void renderCostume(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay) {
        costume.render(poseStack, buffer, packedLight, packedOverlay);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                               int color) {
        renderWings(poseStack, buffer, packedLight, packedOverlay);
        renderMask(poseStack, buffer, packedLight, packedOverlay);
    }
}
