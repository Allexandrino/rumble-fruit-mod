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
        this.alpha = Math.max(0.0F, 1.0F - (float) this.age / (float) this.lifetime);
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
