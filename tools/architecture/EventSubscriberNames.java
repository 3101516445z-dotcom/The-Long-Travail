import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.nio.file.*;
import java.util.*;
import java.util.jar.*;

// 按 EventBus 6.2.33 的代理命名规则检查包名、简单类名、方法名和事件简单名。
public final class EventSubscriberNames {
    public static void main(String[] args) throws Exception {
        Map<String, List<String>> names = new TreeMap<>();
        int methods = 0;
        List<byte[]> classes = new ArrayList<>();
        if (Files.isDirectory(Path.of(args[0]))) {
            try (var paths = Files.walk(Path.of(args[0]))) {
                for (var path : paths.filter(p -> p.toString().endsWith(".class")).toList())
                    classes.add(Files.readAllBytes(path));
            }
        } else
            try (var jar = new JarFile(args[0])) {
                for (var entry : Collections.list(jar.entries())) {
                    if (!entry.getName().endsWith(".class")) continue;
                    try (var input = jar.getInputStream(entry)) {
                        classes.add(input.readAllBytes());
                    }
                }
            }
        for (var bytes : classes) {
            var node = new ClassNode();
            new ClassReader(bytes)
                    .accept(
                            node,
                            ClassReader.SKIP_CODE
                                    | ClassReader.SKIP_DEBUG
                                    | ClassReader.SKIP_FRAMES);
            String owner = node.name.replace('/', '.');
            int split = owner.lastIndexOf('.');
            String simple = owner.substring(split + 1);
            for (var inner : node.innerClasses) {
                if (node.name.equals(inner.name) && inner.innerName != null)
                    simple = inner.innerName;
            }
            for (var method : node.methods) {
                if (method.visibleAnnotations == null
                        || method.visibleAnnotations.stream()
                                .noneMatch(
                                        a ->
                                                a.desc.equals(
                                                        "Lnet/minecraftforge/eventbus/api/SubscribeEvent;")))
                    continue;
                var arguments = Type.getArgumentTypes(method.desc);
                if (arguments.length != 1)
                    throw new AssertionError("Invalid handler: " + owner + "." + method.name);
                String event = arguments[0].getClassName();
                event =
                        event.substring(
                                Math.max(event.lastIndexOf('.'), event.lastIndexOf('$')) + 1);
                String key =
                        owner.substring(0, split)
                                + ".__"
                                + simple
                                + "_"
                                + method.name
                                + "_"
                                + event;
                names.computeIfAbsent(key, ignored -> new ArrayList<>())
                        .add(owner + "." + method.name + method.desc);
                methods++;
            }
        }
        int collisions = 0;
        for (var entry : names.entrySet()) {
            if (entry.getValue().size() < 2) continue;
            collisions++;
            System.out.println(
                    "COLLISION " + entry.getKey() + " => " + String.join(" | ", entry.getValue()));
        }
        System.out.println("Subscriber methods=" + methods + " collisions=" + collisions);
        if (collisions != 0) throw new AssertionError("Event subscriber wrapper name collision");
    }
}
