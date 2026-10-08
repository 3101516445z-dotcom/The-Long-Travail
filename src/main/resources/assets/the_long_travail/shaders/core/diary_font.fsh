#version 150
uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float Seconds;
uniform float Material;
uniform float AtlasMode;
uniform float SweepWidth;
uniform vec3 ThemeDark;
uniform vec3 ThemeMain;
uniform vec3 ThemeHighlight;
uniform float Barren;
in vec4 vertexColor;
in vec2 texCoord0;
in float textX;
out vec4 fragColor;

vec3 nameColor() {
    float angle = fract(Seconds / 4.0) * 6.2831853;
    vec2 p = vec2(textX, gl_FragCoord.y * 0.5);
    float grain = 0.5 + 0.5 * sin(p.x * 1.7 + sin(p.y * 0.9) * 1.8);
    float bend = sin(p.y * 0.73 + p.x * 0.21) * 0.65;
    float distance = abs(sin((p.x * 0.24 - angle + bend) * 0.5));
    float vein = 1.0 - smoothstep(0.04, 0.22, distance);
    float halo = 1.0 - smoothstep(0.16, 0.62, distance);
    vec3 color = mix(vec3(0.43, 0.27, 0.10), vec3(0.66, 0.45, 0.19), grain);
    color = mix(color, vec3(0.90, 0.67, 0.29), halo * 0.65);
    color = mix(color, vec3(1.0, 0.90, 0.61), vein * 0.94);

    vec2 cell = floor(p / 3.5);
    vec2 local = fract(p / 3.5) - 0.5;
    float seed = fract(sin(dot(cell, vec2(127.1, 311.7))) * 43758.5453);
    float point = step(0.83, seed) * (1.0 - smoothstep(0.04, 0.30, length(local)));
    float pulse = 0.5 + 0.5 * sin(angle + seed * 6.2831853);
    return mix(color, vec3(1.0, 0.96, 0.77), point * pulse * halo * 0.80);
}

vec3 aspectNameColor() {
    float angle = fract(Seconds / mix(4.0, 9.0, Barren)) * 6.2831853;
    vec2 p = vec2(textX, gl_FragCoord.y * 0.5);
    float grain = 0.5 + 0.5 * sin(p.x * 1.7 + sin(p.y * 0.9) * 1.8);
    float bend = sin(p.y * 0.73 + p.x * 0.21) * 0.65;
    float distance = abs(sin((p.x * 0.24 - angle + bend) * 0.5));
    float vein = 1.0 - smoothstep(0.02, mix(0.22, 0.10, Barren), distance);
    float glow = 1.0 - smoothstep(0.16, 0.62, distance);
    vec3 color = mix(ThemeDark, ThemeMain, 0.35 + grain * 0.65);
    color = mix(color, ThemeMain, glow * mix(0.65, 0.25, Barren));
    color = mix(color, ThemeHighlight, vein * mix(0.94, 0.72, Barren));
    vec2 cell = floor(p / 3.5);
    vec2 local = fract(p / 3.5) - 0.5;
    float seed = fract(sin(dot(cell, vec2(127.1, 311.7))) * 43758.5453);
    float point = step(mix(0.83, 0.96, Barren), seed) * (1.0 - smoothstep(0.04, 0.30, length(local)));
    float pulse = 0.5 + 0.5 * sin(angle + seed * 6.2831853);
    return mix(color, ThemeHighlight, point * pulse * glow * 0.80);
}

vec3 proseColor(float phase) {
    vec3 bronze = vec3(0.68, 0.51, 0.29);
    vec3 ochre = vec3(0.74, 0.60, 0.37);
    vec3 parchmentGold = vec3(0.81, 0.68, 0.44);
    float stage = fract(phase) * 3.0;
    float blend = smoothstep(0.0, 1.0, fract(stage));
    if (stage < 1.0) return mix(bronze, ochre, blend);
    if (stage < 2.0) return mix(ochre, parchmentGold, blend);
    return mix(parchmentGold, bronze, blend);
}

void main() {
    vec4 glyph = texture(Sampler0, texCoord0);
    float coverage = AtlasMode > 0.5 && AtlasMode < 1.5 ? glyph.r : glyph.a;
    if (AtlasMode > 1.5) {
        float distance = glyph.a - 127.0 / 255.0 + 0.04;
        coverage = clamp(distance / max(fwidth(distance), 0.0001) + 0.5, 0.0, 1.0);
    }
    float alpha = coverage * vertexColor.a * ColorModulator.a;
    if (alpha < 0.01) discard;
    float brightness = max(vertexColor.r, max(vertexColor.g, vertexColor.b));
    float shadow = brightness < 0.35 ? 0.25 : 1.0;
    vec3 color;
    if (Material > 8.5) {
        float ripple = 0.5 + 0.5 * sin(textX * 0.48 - Seconds * 1.5707963);
        float swell = 0.5 + 0.5 * sin(Seconds * 1.0471976);
        alpha *= 0.55 + 0.30 * ripple + 0.15 * swell;
        color = mix(ThemeMain, ThemeHighlight, ripple * 0.6);
    } else if (Material > 2.5) {
        color = aspectNameColor();
    } else if (Material > 1.5) {
        float ripple = 0.5 + 0.5 * sin(textX * 0.48 - Seconds * 1.5707963);
        float swell = 0.5 + 0.5 * sin(Seconds * 1.0471976);
        alpha *= 0.55 + 0.30 * ripple + 0.15 * swell;
        color = mix(vec3(0.72, 0.43, 0.13), vec3(1.0, 0.79, 0.38), ripple * 0.6);
    } else if (Material < 0.5) {
        color = nameColor();
    } else {
        // 在文本范围之外重置光带，使其在十秒内完整移入再移出。
        float center = mix(-0.14, 1.14, fract(Seconds / 10.0));
        float sheen = 1.0 - smoothstep(0.025, 0.12, abs(textX / max(SweepWidth, 1.0) - center));
        vec3 ink = proseColor(Seconds / 8.0 - textX / 110.0);
        color = mix(ink, min(ink + vec3(0.07, 0.065, 0.05), vec3(1.0)), sheen);
    }
    // 保留字体渲染器的阴影通道，避免将材质标记染色。
    fragColor = vec4(color * shadow * ColorModulator.rgb, alpha);
}
