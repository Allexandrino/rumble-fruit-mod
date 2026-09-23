package com.rumblefruit;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

// the Fallen Exorcist rendered as what it is: a 50-block colossus assembled
// from hundreds of real cubes — a blackstone statue with a gold chest plate,
// crying-obsidian arms and head, a burning golden halo and sweeping quartz
// wings of a broken archangel
public class FallenExorcistRenderer extends EntityRenderer<FallenExorcistEntity> {
    private final BlockRenderDispatcher blockRenderer;

    public FallenExorcistRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
        this.shadowRadius = 4.0F;
    }

    @Override
    public void render(FallenExorcistEntity entity, float entityYaw, float partialTicks,
                       PoseStack pose, MultiBufferSource buffer, int packedLight) {
        pose.pushPose();
        pose.scale(2.0F, 2.0F, 2.0F); // statue is ~26 units -> ~52 blocks tall
        // rotate the whole statue with the body yaw
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F - entityYaw));

        // legs
        box(pose, buffer, Blocks.BLACKSTONE, -1, 0, -1, 0, 6, 0);
        box(pose, buffer, Blocks.BLACKSTONE, 1, 0, -1, 2, 6, 0);
        // pelvis
        box(pose, buffer, Blocks.GILDED_BLACKSTONE, -1, 7, -1, 2, 7, 0);
        // torso
        box(pose, buffer, Blocks.BLACKSTONE, -2, 8, -1, 3, 13, 1);
        // gold chest plate on the front
        box(pose, buffer, Blocks.GOLD_BLOCK, -1, 10, 2, 2, 12, 2);
        // arms hanging
        box(pose, buffer, Blocks.CRYING_OBSIDIAN, -4, 8, -1, -3, 13, 0);
        box(pose, buffer, Blocks.CRYING_OBSIDIAN, 4, 8, -1, 5, 13, 0);
        // shoulders
        box(pose, buffer, Blocks.GOLD_BLOCK, -4, 14, -1, -3, 14, 0);
        box(pose, buffer, Blocks.GOLD_BLOCK, 4, 14, -1, 5, 14, 0);
        // head
        box(pose, buffer, Blocks.CRYING_OBSIDIAN, -1, 14, -1, 2, 16, 1);
        // burning golden eyes on the front face
        box(pose, buffer, Blocks.GLOWSTONE, 0, 15, 2, 0, 15, 2);
        box(pose, buffer, Blocks.GLOWSTONE, 1, 15, 2, 1, 15, 2);
        // halo: a broken golden ring floating above the head
        float time = entity.tickCount + partialTicks;
        for (int i = 0; i < 8; i++) {
            double a = time * 0.02F + i * Math.PI / 4.0;
            blockAt(pose, buffer, Blocks.GOLD_BLOCK,
                    (int) Math.round(Math.cos(a) * 2.0), 18, (int) Math.round(Math.sin(a) * 2.0));
        }
        // broken archangel wings: quartz arcs sweeping out and up, gold tips
        wing(pose, buffer, -1, time);
        wing(pose, buffer, 1, time);

        pose.popPose();
        super.render(entity, entityYaw, partialTicks, pose, buffer, packedLight);
    }

    private void wing(PoseStack pose, MultiBufferSource buffer, int side, float time) {
        // an arc of quartz rising from the shoulder; the tip feathers are gold
        float flap = (float) Math.sin(time * 0.05F) * 0.5F;
        for (int i = 1; i <= 8; i++) {
            int x = side * (3 + i);
            int y = 13 + (int) (i * 0.8 + flap * i * 0.2);
            Block block = i >= 6 ? Blocks.GOLD_BLOCK : Blocks.QUARTZ_BLOCK;
            blockAt(pose, buffer, block, x, y, -1);
            if (i >= 3) {
                blockAt(pose, buffer, Blocks.QUARTZ_BLOCK, x, y - 1, -1); // feather underside
            }
        }
    }

    private void box(PoseStack pose, MultiBufferSource buffer, Block block,
                     int x0, int y0, int z0, int x1, int y1, int z1) {
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    blockAt(pose, buffer, block, x, y, z);
                }
            }
        }
    }

    private void blockAt(PoseStack pose, MultiBufferSource buffer, Block block, int x, int y, int z) {
        pose.pushPose();
        pose.translate(x, y, z);
        blockRenderer.renderSingleBlock(block.defaultBlockState(),
                pose, buffer, 0xF000F0, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(FallenExorcistEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
