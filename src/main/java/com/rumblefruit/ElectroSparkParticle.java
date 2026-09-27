package com.rumblefruit;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import org.lwjgl.opengl.GL11;

// the everyday electro particle: a small additive glow-quad that drifts,
// shines fullbright and fades out. used for every simple particle of the mod
// (sparks of all elements, glow, cloud)
public class ElectroSparkParticle extends TextureSheetParticle {
    private static final ParticleRenderType ADDITIVE = new ParticleRenderType() {
        @Override
        public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
            RenderSystem.depthMask(false);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public String toString() {
            return "ELECTRO_SPARK";
        }
    };

    protected ElectroSparkParticle(ClientLevel level, double x, double y, double z,
                                   double dx, double dy, double dz, SpriteSet sprites,
                                   float size, int lifetime) {
        super(level, x, y, z, dx, dy, dz);
        this.quadSize = size;
        this.lifetime = lifetime + this.random.nextInt(6);
        this.hasPhysics = false;
        this.setSprite(sprites.get(0, 1));
    }

    public static ElectroSparkParticle spark(ClientLevel level, double x, double y, double z,
                                             double dx, double dy, double dz, SpriteSet sprites) {
        return new ElectroSparkParticle(level, x, y, z, dx, dy, dz, sprites, 0.16F, 14);
    }

    public static ElectroSparkParticle glow(ClientLevel level, double x, double y, double z,
                                            double dx, double dy, double dz, SpriteSet sprites) {
        return new ElectroSparkParticle(level, x, y, z, dx, dy, dz, sprites, 0.30F, 26);
    }

    public static ElectroSparkParticle cloud(ClientLevel level, double x, double y, double z,
                                             double dx, double dy, double dz, SpriteSet sprites) {
        ElectroSparkParticle particle = new ElectroSparkParticle(level, x, y, z, dx, dy, dz, sprites,
                1.2F, 40);
        particle.alpha = 0.45F;
        return particle;
    }

    @Override
    public void tick() {
        super.tick();
    }

    // the particle burns on a timeline: fast attack -> hot peak -> slow decay,
    // rendered in two layers (soft halo + searing core), both additive
    @Override
    public void render(com.mojang.blaze3d.vertex.VertexConsumer buffer,
                       net.minecraft.client.Camera camera, float partialTicks) {
        float t = Math.min(1.0F, (this.age + partialTicks) / (float) this.lifetime);
        // alpha timeline: snap in, blaze, fade
        float alphaMul = t < 0.15F ? t / 0.15F : Math.max(0.0F, 1.0F - (t - 0.15F) / 0.85F);
        // scale timeline: punch out, then settle
        float scaleMul = t < 0.2F ? 0.6F + t * 2.5F : 1.1F - t * 0.4F;
        float baseSize = this.quadSize;
        float baseAlpha = this.alpha;
        // halo layer: wide and soft
        this.quadSize = baseSize * 2.1F * scaleMul;
        this.alpha = baseAlpha * 0.35F * alphaMul;
        super.render(buffer, camera, partialTicks);
        // core layer: hot and tight
        this.quadSize = baseSize * scaleMul;
        this.alpha = baseAlpha * alphaMul;
        super.render(buffer, camera, partialTicks);
        this.quadSize = baseSize;
        this.alpha = baseAlpha;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 15728880; // fullbright — the sparks glow in the dark
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ADDITIVE;
    }
}
