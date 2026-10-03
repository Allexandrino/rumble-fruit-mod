package com.rumblefruit;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

// the Fallen Exorcist, 50 blocks tall: a colossal horned face with four vast
// wings looming out of a mound of living darkness — now driven by GeckoLib
// keyframe/Molang animation instead of hand-rolled per-tick math.
// AutoGlowingGeoLayer picks up exorcist_glow.png automatically: the eyes,
// the core and the cracks in the dark burn with an emissive pass
public class FallenExorcistRenderer extends GeoEntityRenderer<FallenExorcistEntity> {
    public FallenExorcistRenderer(EntityRendererProvider.Context context) {
        super(context, new GeckoExorcistModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        this.shadowRadius = 8.0F;
        withScale(30.8F); // 26-unit model -> 50-block colossus
    }
}
