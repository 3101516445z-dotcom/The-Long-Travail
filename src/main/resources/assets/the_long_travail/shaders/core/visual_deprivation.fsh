#version 150
uniform vec4 Veil;
uniform float Softness;
in vec2 screenPosition;
out vec4 fragColor;
void main() {
    float edge = smoothstep(Veil.w, Veil.w + Softness, length(screenPosition));
    float opacity = 1.0 - (1.0 - Veil.y) * (1.0 - Veil.z * edge);
    fragColor = vec4(0.0, 0.0, 0.0, clamp(opacity * Veil.x, 0.0, 1.0));
}
