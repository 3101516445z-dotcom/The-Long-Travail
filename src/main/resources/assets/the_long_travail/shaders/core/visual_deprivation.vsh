#version 150
in vec3 Position;
out vec2 screenPosition;
void main() { gl_Position = vec4(Position, 1.0); screenPosition = Position.xy; }
