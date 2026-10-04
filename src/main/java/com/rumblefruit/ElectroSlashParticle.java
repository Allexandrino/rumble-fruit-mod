package com.rumblefruit;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;

public class ElectroSlashParticle extends TextureSheetParticle {
    private static final ParticleRenderType ADDITIVE_SHEET = new ParticleRenderType() {
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
            return "ELECTRO_SLASH";
        }
    };

    private final Quaternionf orientation;

    // yawDeg/pitchDeg/rollDeg arrive through the packet's speed channels (count=0 spawn)
    protected ElectroSlashParticle(ClientLevel level, double x, double y, double z,
                                   double yawDeg, double pitchDeg, double rollDeg, SpriteSet sprites) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.lifetime = 10 + this.random.nextInt(3);
        this.quadSize = 3.0F;
        this.hasPhysics = false;
        Quaternionf q = new Quaternionf().rotateY((float) Math.toRadians(-yawDeg));
        q.mul(new Quaternionf().rotateX((float) Math.toRadians(-pitchDeg)));
        q.mul(new Quaternionf().rotateZ((float) Math.toRadians(rollDeg)));
        this.orientation = q;
        this.setSprite(sprites.get(0, 1));
    }

    @Override
    public void tick() {
        super.tick();
        this.quadSize *= 1.05F;
        this.alpha = 1.0F - (float) this.age / (float) this.lifetime;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 15728880;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ADDITIVE_SHEET;
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        Vec3 camPos = camera.getPosition();
        float f = (float) (Mth.lerp(partialTicks, this.xo, this.x) - camPos.x());
        float f1 = (float) (Mth.lerp(partialTicks, this.yo, this.y) - camPos.y());
        float f2 = (float) (Mth.lerp(partialTicks, this.zo, this.z) - camPos.z());

        // world-fixed slash plane: quad corners rotated by the swing orientation, not the camera
        Vector3f[] corners = {
                new Vector3f(-1.0F, -1.0F, 0.0F),
                new Vector3f(-1.0F, 1.0F, 0.0F),
                new Vector3f(1.0F, 1.0F, 0.0F),
                new Vector3f(1.0F, -1.0F, 0.0F)};
        float size = this.getQuadSize(partialTicks);
        for (Vector3f corner : corners) {
            corner.rotate(this.orientation);
            corner.mul(size);
            corner.add(f, f1, f2);
        }

        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();
        int light = this.getLightColor(partialTicks);
        buffer.addVertex(corners[0].x(), corners[0].y(), corners[0].z()).setUv(u1, v1)
                .setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(light);
        buffer.addVertex(corners[1].x(), corners[1].y(), corners[1].z()).setUv(u1, v0)
                .setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(light);
        buffer.addVertex(corners[2].x(), corners[2].y(), corners[2].z()).setUv(u0, v0)
                .setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(light);
        buffer.addVertex(corners[3].x(), corners[3].y(), corners[3].z()).setUv(u0, v1)
                .setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(light);
    }
}
