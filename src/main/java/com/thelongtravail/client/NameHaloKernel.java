package com.thelongtravail.client;

import com.thelongtravail.config.TravailClientConfig.HaloQuality;

public final class NameHaloKernel {
    private static final NameHaloKernel[] KERNELS = java.util.Arrays.stream(HaloQuality.values())
            .map(NameHaloKernel::new).toArray(NameHaloKernel[]::new);
    private final float[][] x, y;
    private final float[] opacity;
    public final int samplesPerRing;

    private NameHaloKernel(HaloQuality quality) {
        samplesPerRing = quality.samplesPerRing;
        x = new float[2][samplesPerRing]; y = new float[2][samplesPerRing];
        opacity = new float[]{quality.opacity(0.075F), quality.opacity(0.035F)};
        if (samplesPerRing == 0) return;
        int stride = 8 / samplesPerRing;
        for (int ring = 0; ring < 2; ring++) for (int sample = 0; sample < samplesPerRing; sample++) {
            int index = (sample * stride + (ring == 0 ? 0 : stride / 2)) % 8;
            double angle = (index + ring * 0.5) * Math.PI / 4;
            float radius = ring == 0 ? 0.55F : 1.1F;
            x[ring][sample] = radius * (float) Math.cos(angle);
            y[ring][sample] = radius * (float) Math.sin(angle);
        }
    }
    public static NameHaloKernel of(HaloQuality quality) { return KERNELS[quality.ordinal()]; }
    public int alpha(int color, int ring) {
        int alpha = color >>> 24;
        if (alpha < 4) alpha = 255; // Font 的隐式不透明颜色约定。
        return Math.round(alpha * opacity[ring]);
    }
    public float x(int ring, int sample) { return x[ring][sample]; }
    public float y(int ring, int sample) { return y[ring][sample]; }
}
