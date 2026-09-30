package com.thelongtravail.client;

/** Renderer-independent parameters, interpolated after configuration changes. */
public record VisualDeprivationStyle(float darkening, float opacity, float radius, float softness,
                                     float distanceEnabled, float distanceStart, float distanceEnd,
                                     float pulsePeriod, float pulseDepth, float pulseRadius) {
    public VisualDeprivationStyle blend(VisualDeprivationStyle to, float progress) {
        if (progress >= 1) return to;
        if (progress <= 0) return this;
        float t = VisualDeprivationTimeline.smooth(progress);
        return new VisualDeprivationStyle(mix(darkening,to.darkening,t),mix(opacity,to.opacity,t),
                mix(radius,to.radius,t),mix(softness,to.softness,t),mix(distanceEnabled,to.distanceEnabled,t),
                mix(distanceStart,to.distanceStart,t),mix(distanceEnd,to.distanceEnd,t),
                mix(pulsePeriod,to.pulsePeriod,t),mix(pulseDepth,to.pulseDepth,t),mix(pulseRadius,to.pulseRadius,t));
    }
    private static float mix(float a, float b, float t) { return a + (b-a)*t; }
    public float pulse(float phase, boolean reducedMotion) {
        return reducedMotion ? 1 : 1 - pulseDepth * (0.5F - 0.5F * (float) Math.cos(phase));
    }
}
