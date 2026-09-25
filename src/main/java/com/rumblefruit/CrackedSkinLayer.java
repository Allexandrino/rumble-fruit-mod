package com.rumblefruit;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

// the elemental transformation cracks the skin: a web of glowing cracks in
// the fruit's color crawls over the whole body, and the abyss of the same
// color stares out of them (dark void core inside every crack).
// rendered as an animated energy-swirl overlay, like the charged creeper shell
public class CrackedSkinLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation[] TEXTURES = {
            tex("lightning"), tex("inferno"), tex("void"), tex("frost"), tex("nature")
    };

    private static ResourceLocation tex(String element) {
        return ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID,
                "textures/entity/cracks_" + element + ".png");
    }

    public CrackedSkinLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!ClientWingsData.isActive(player.getUUID())) {
            return;
        }
        ResourceLocation texture = TEXTURES[com.rumblefruit.core.ElementCatalog
                .byId(ClientPowerData.element()).id()];
        var element = com.rumblefruit.core.ElementCatalog.byId(ClientPowerData.element());
        // pass 1: the abyss — the crack cores sunk INTO the flesh, near-black
        // with a breath of the element color (reads as bottomless holes)
        int abyss = 0xFF000000 | ((element.color() >> 3) & 0x1F1F1F);
        VertexConsumer deep = buffer.getBuffer(RenderType.entityTranslucent(texture));
        poseStack.pushPose();
        poseStack.scale(0.97F, 0.975F, 0.97F); // recessed beneath the skin
        this.getParentModel().renderToBuffer(poseStack, deep,
                0xF000F0, OverlayTexture.NO_OVERLAY, abyss);
        poseStack.popPose();
        // pass 2: the glowing rims of every crack on the surface
        VertexConsumer rim = buffer.getBuffer(RenderType.entityTranslucent(texture));
        this.getParentModel().renderToBuffer(poseStack, rim,
                0xF000F0, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
    }
}
