package raccoonman.reterraforged.world.worldgen.cell.beach;

import raccoonman.reterraforged.world.worldgen.cell.heightmap.ControlPoints;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;

public final class ShoreGeometry {
    private static final float MIN_SCALE = 0.25F;

    private ShoreGeometry() {
    }

    public static float getOceanNormalizedDistance(float continentEdge, ControlPoints controlPoints, float coastBandScale) {
        float scale = Math.max(MIN_SCALE, coastBandScale);
        float shallowOcean = controlPoints.shallowOcean();
        float coast = controlPoints.coast();
        float coastMarker = controlPoints.coastMarker();
        float primaryRange = Math.max(0.0001F, coast - shallowOcean);
        float shoulderRange = Math.max(0.0001F, coastMarker - coast);
        float extraScale = Math.max(0.0F, scale - 1.0F);
        float extraRange = Math.min(shoulderRange, primaryRange * extraScale);
        float span = primaryRange + extraRange;
        return NoiseUtil.clamp((continentEdge - shallowOcean) / span, 0.0F, 1.0F);
    }

    public static float applyBias(float alpha, float bias) {
        alpha = NoiseUtil.clamp(alpha, 0.0F, 1.0F);
        bias = NoiseUtil.clamp(bias, -1.0F, 1.0F);
        float exponent = bias >= 0.0F ? NoiseUtil.lerp(1.0F, 0.5F, bias) : NoiseUtil.lerp(1.0F, 2.0F, -bias);
        return NoiseUtil.clamp((float) Math.pow(alpha, exponent), 0.0F, 1.0F);
    }
}
