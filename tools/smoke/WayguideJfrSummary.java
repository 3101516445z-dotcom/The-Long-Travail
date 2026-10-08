package travail.smoke;

import jdk.jfr.consumer.*;
import java.nio.file.Path;
import java.util.*;

// 流式读取等待栈，避免把 JFR 的完整类型元数据展开为巨大的 JSON。
public final class WayguideJfrSummary {
    public static void main(String[] args) throws Exception {
        Map<String, Long> counts = new TreeMap<>(), duration = new TreeMap<>(), executions = new HashMap<>();
        Map<String, List<String>> examples = new TreeMap<>();
        int gc = 0;
        try (var recording = new RecordingFile(Path.of(args[0]))) {
            while (recording.hasMoreEvents()) {
                var event = recording.readEvent();
                String type = event.getEventType().getName();
                if (type.equals("jdk.GarbageCollection")) { gc++; continue; }
                var trace = event.getStackTrace();
                if (trace == null) continue;
                List<String> stack = trace.getFrames().stream()
                        .map(f -> f.getMethod().getType().getName() + "." + f.getMethod().getName()).toList();
                String joined = String.join("\n", stack);
                if (!joined.contains("com.thelongtravail.data.WayguideSearch")) continue;
                var thread = event.hasField("eventThread") ? event.getThread("eventThread") : event.getThread("sampledThread");
                if (thread == null || !thread.getJavaName().equals("Server thread")) continue;
                if (type.equals("jdk.ThreadPark")) {
                    String category = joined.contains("tryLoadFromStorage") ? "structure_metadata_IO" : joined.contains("getChunk") ? "synchronous_chunk_request" : "other";
                    category += joined.contains("WayguideSearch.use") ? "/right_click" : "/search_tick";
                    counts.merge(category, 1L, Long::sum); duration.merge(category, event.getDuration().toNanos(), Long::sum);
                    examples.putIfAbsent(category, stack);
                } else if (type.equals("jdk.ExecutionSample")) {
                    stack.stream().filter(f -> f.contains("thelongtravail") || f.contains("StructureCheck") || f.contains("StructureTemplate") || f.contains("JigsawPlacement") || f.contains("RegionFile"))
                            .findFirst().ifPresent(f -> executions.merge(f, 1L, Long::sum));
                }
            }
        }
        counts.forEach((category, count) -> {
            System.out.printf(Locale.ROOT, "%s: parks=%d, total_wait_ms=%.3f%n", category, count, duration.get(category) / 1_000_000.0);
            examples.get(category).stream().limit(25).forEach(f -> System.out.println("  " + f));
        });
        System.out.println("Execution samples in Wayguide paths:");
        executions.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(15).forEach(System.out::println);
        System.out.println("GC events in entire live recording: " + gc);
    }
}
