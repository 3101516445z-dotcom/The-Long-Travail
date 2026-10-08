import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.lwjgl.opengl.GL33.*;

// 在隐藏的真实 OpenGL 上下文中编译并渲染实际发布的着色器。
public class DiaryFontShaderTest {
    static int program, texture;
    static int shader(int kind, Path path) throws Exception {
        int id = glCreateShader(kind);
        glShaderSource(id, Files.readString(path));
        glCompileShader(id);
        if (glGetShaderi(id, GL_COMPILE_STATUS) == GL_FALSE)
            throw new AssertionError(path + ": " + glGetShaderInfoLog(id));
        return id;
    }
    static void uniform(String name, float value) { glUniform1f(glGetUniformLocation(program, name), value); }
    static int[] sample(float time, float material, float atlas, int red, int alpha) {
        ByteBuffer pixel = MemoryUtil.memAlloc(4);
        try {
            pixel.put((byte) red).put((byte) 255).put((byte) 255).put((byte) alpha).flip();
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, 1, 1, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixel);
            uniform("Seconds", time); uniform("Material", material); uniform("AtlasMode", atlas);
            glClearColor(0, 0, 0, 0); glClear(GL_COLOR_BUFFER_BIT);
            glDrawArrays(GL_TRIANGLES, 0, 6);
            pixel.clear(); glReadPixels(16, 16, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, pixel);
            return new int[]{pixel.get(0) & 255, pixel.get(1) & 255, pixel.get(2) & 255, pixel.get(3) & 255};
        } finally { MemoryUtil.memFree(pixel); }
    }
    static void check(boolean value, String reason) { if (!value) throw new AssertionError(reason); }
    static void color(String name, int rgb) {
        glUniform3f(glGetUniformLocation(program, name), ((rgb >> 16) & 255)/255f, ((rgb >> 8) & 255)/255f, (rgb & 255)/255f);
    }
    static int proseRed(float seconds, float x) {
        double phase = seconds / 8.0 - x / 110.0;
        double stage = (phase - Math.floor(phase)) * 3;
        int part = (int) stage;
        double t = stage - part;
        t = t * t * (3 - 2 * t);
        double[] colors = {0.68, 0.74, 0.81};
        return (int) Math.round(255 * (colors[part] * (1-t) + colors[(part+1)%3] * t));
    }
    public static void main(String[] args) throws Exception {
        check(GLFW.glfwInit(), "GLFW initialization failed");
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        long window = GLFW.glfwCreateWindow(32, 32, "Diary shader test", 0, 0);
        check(window != 0, "Hidden GL context creation failed");
        try {
            GLFW.glfwMakeContextCurrent(window); GL.createCapabilities();
            Path root = Path.of("src/main/resources/assets/the_long_travail/shaders/core");
            program = glCreateProgram();
            glAttachShader(program, shader(GL_VERTEX_SHADER, root.resolve("diary_font.vsh")));
            glAttachShader(program, shader(GL_FRAGMENT_SHADER, root.resolve("diary_font.fsh")));
            glBindAttribLocation(program, 0, "Position"); glBindAttribLocation(program, 1, "Color");
            glBindAttribLocation(program, 2, "UV0"); glLinkProgram(program);
            check(glGetProgrami(program, GL_LINK_STATUS) != GL_FALSE, glGetProgramInfoLog(program));
            glUseProgram(program);
            float[] identity = {1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1};
            glUniformMatrix4fv(glGetUniformLocation(program, "ModelViewMat"), false, identity);
            glUniformMatrix4fv(glGetUniformLocation(program, "ProjMat"), false, identity);
            glUniform4f(glGetUniformLocation(program, "ColorModulator"), 1,1,1,1);
            glUniform1i(glGetUniformLocation(program, "Sampler0"), 0);
            glBindVertexArray(glGenVertexArrays()); glBindBuffer(GL_ARRAY_BUFFER, glGenBuffers());
            glBufferData(GL_ARRAY_BUFFER, new float[]{-1,-1,0, 1,-1,0, 1,1,0, -1,-1,0, 1,1,0, -1,1,0}, GL_STATIC_DRAW);
            glEnableVertexAttribArray(0); glVertexAttribPointer(0,3,GL_FLOAT,false,12,0);
            glVertexAttrib4f(1,1,1,1,1); glVertexAttrib2f(2,0.5f,0.5f);
            texture = glGenTextures(); glBindTexture(GL_TEXTURE_2D, texture);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glViewport(0,0,32,32);
            int[] first = sample(0,0,0,255,255), loop = sample(4,0,0,255,255);
            check(java.util.Arrays.equals(first, loop), "Name cycle must close after four seconds");
            int nameMin = 255, nameMax = 0;
            for (int frame = 0; frame < 80; frame++) {
                int[] ink = sample(frame / 20f,0,0,255,255);
                nameMin = Math.min(nameMin, ink[0]); nameMax = Math.max(nameMax, ink[0]);
                check(ink[0] >= ink[1] && ink[1] >= ink[2], "Name stays in the gold palette");
            }
            check(nameMax-nameMin > 60, "Gold veins must be clearly visible");
            for (int atlas = 0; atlas < 3; atlas++) {
                check(sample(0,0,atlas,255,255)[3] == 255, "Solid glyph coverage, atlas " + atlas);
                check(sample(0,0,atlas,0,0)[3] == 0, "Transparent glyph coverage, atlas " + atlas);
            }
            glVertexAttrib4f(1,188/255f,153/255f,94/255f,1);
            uniform("SweepWidth", 256);
            uniform("OriginX", 0.03125f - 128);
            int[] proseStart = sample(0,1,0,255,255);
            check(java.util.Arrays.equals(proseStart, sample(40,1,0,255,255)), "Combined 8s gradient and 10s sheen repeat after 40s");
            check(proseStart[0] > proseStart[1] && proseStart[1] > proseStart[2], "Brown-gold palette");
            for (float fraction : new float[]{0.1f,0.5f,0.9f}) {
                uniform("OriginX", 0.03125f - 256 * fraction);
                float peakTime = 10 * (fraction + 0.14f) / 1.28f;
                int peak = sample(peakTime,1,0,255,255)[0];
                int base = proseRed(peakTime, 256 * fraction);
                check(peak-base >= 16 && peak-base <= 20, "Left-to-right sweep timing at " + fraction);
            }
            for (float edge : new float[]{0,1}) {
                uniform("OriginX", 0.03125f - 256 * edge);
                for (float time : new float[]{0,9.999f,10,10.001f}) {
                    int actual = sample(time,1,0,255,255)[0];
                    check(Math.abs(actual-proseRed(time,256*edge)) <= 1, "No sheen flash at wrap boundary");
                }
            }
            uniform("OriginX", 0.03125f - 128);
            int min = 255, max = 0;
            for (int tick = 0; tick <= 100; tick++) {
                int red = sample(tick / 10f,1,0,255,255)[0];
                min = Math.min(min,red); max = Math.max(max,red);
            }
            check(max-min >= 20 && max-min <= 55, "Restrained same-hue gradient and sheen: " + (max-min));
            glVertexAttrib4f(1,168/255f,133/255f,50/255f,1);
            int[] playerInk = sample(0,1,0,255,255);
            check(java.util.Arrays.equals(playerInk, proseStart), "Player name shares the same prose material");
            glVertexAttrib4f(1,1,1,1,1);
            uniform("OriginX", 0);
            var outputs = new java.util.HashSet<String>();
            for (var theme : com.thelongtravail.AspectTheme.values()) {
                color("ThemeDark", theme.dark); color("ThemeMain", theme.main); color("ThemeHighlight", theme.highlight);
                uniform("Barren", theme == com.thelongtravail.AspectTheme.FAR_REACH ? 1 : 0);
                var frames = new java.util.HashSet<String>();
                for (int frame = 0; frame < 90; frame++)
                    frames.add(java.util.Arrays.toString(sample(frame / 10f, theme.material(), 0, 255, 255)));
                check(frames.size() >= 3, "Theme must show multiple animation colors: " + theme);
                outputs.add(java.util.Arrays.toString(sample(0, theme.material(), 0, 255, 255)));
                for (int atlas = 0; atlas < 3; atlas++) {
                    check(sample(0,theme.material(),atlas,255,255)[3] == 255, "Theme solid coverage: " + theme);
                    check(sample(0,theme.material(),atlas,0,0)[3] == 0, "Theme transparent coverage: " + theme);
                    int[] halo = sample(0,theme.material()+6,atlas,255,255);
                    check(halo[3] > 0 && halo[3] < 255, "Soft theme halo coverage: " + theme);
                    check(sample(0,theme.material()+6,atlas,0,0)[3] == 0, "Theme halo outside glyph: " + theme);
                    check(!java.util.Arrays.equals(halo, sample(1,theme.material()+6,atlas,255,255)), "Animated theme halo: " + theme);
                }
            }
            check(outputs.size() == 6, "Six themes must have distinct colors");
            check(glGetError() == GL_NO_ERROR, "OpenGL error");
            System.out.println("PASS: six distinct animated aspect palettes and halos, bitmap/intensity/SDF coverage");
            System.out.println("PASS: GLSL compile/link, 4s gold-foil cycle and contrast, 10s off-text prose sweep, uniform brown-gold prose, bitmap/intensity/SDF coverage; " + glGetString(GL_RENDERER));
        } finally { GLFW.glfwDestroyWindow(window); GLFW.glfwTerminate(); }
    }
}
