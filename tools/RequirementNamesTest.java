import com.thelongtravail.data.RequirementNames;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.HashMap;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import com.thelongtravail.data.WeightedEntry;
import com.thelongtravail.client.DiaryPageProse;
import com.thelongtravail.TravailAspect;

public final class RequirementNamesTest {
    public static void main(String[] args) throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/thelongtravail/data/RequirementNames.java"));
        var entries = Pattern.compile("Map.entry\\(\"([^\"]+)\", \"([^\"]+)\"\\)").matcher(source);
        int count = 0;
        while (entries.find()) {
            String id = entries.group(1), name = entries.group(2);
            String expected = id.equals("minecraft:deep_ocean") ? "biome.the_long_travail.warm_deep_ocean"
                    : "biome." + id.replace(':', '.');
            check(expected.equals(RequirementNames.translationKey(false, id, name)), "legacy default " + id);
            check(expected.equals(RequirementNames.translationKey(false, id, "@" + expected)), "explicit key " + id);
            check(RequirementNames.translationKey(false, id, name + " (custom)") == null, "custom name " + id);
            check(RequirementNames.translationKey(true, id, name) == null, "structure must not inherit biome aliases");
            count++;
        }
        check(count == 36, "all legacy defaults covered");
        check(RequirementNames.translationKey(false, "example:plains", "平原") == null, "namespace must match");
        check("structure.example.tower".equals(RequirementNames.translationKey(true, "example:tower", "@structure.example.tower")), "custom structure key");
        check(RequirementNames.translationKey(false, "minecraft:plains", "@") == null, "bare marker remains literal");
        var entry = WeightedEntry.parse("example:stronghold|@structure.example.stronghold|3");
        check(entry != null && entry.weight() == 3 && entry.id().toString().equals("example:stronghold"), "documented format preserves ID and weight");
        Language previous = Language.getInstance();
        try {
            for (String locale : new String[]{"zh_cn", "en_us", "zh_cn"}) {
                Map<String, String> translations = new HashMap<>();
                try (var input = Files.newInputStream(Path.of("src/main/resources/assets/the_long_travail/lang/" + locale + ".json"))) {
                    Language.loadFromJson(input, translations::put);
                }
                translations.put("structure.example.stronghold", locale.equals("zh_cn") ? "末路之门" : "Gate at Journey's End");
                Language.inject(new Language() {
                    public String getOrDefault(String key, String fallback) { return translations.getOrDefault(key, fallback); }
                    public boolean has(String key) { return translations.containsKey(key); }
                    public boolean isDefaultRightToLeft() { return false; }
                    public FormattedCharSequence getVisualOrder(FormattedText text) { return FormattedCharSequence.EMPTY; }
                });
                check(RequirementNames.resolve(true, entry.id(), entry.displayName()).equals(translations.get("structure.example.stronghold")), "translated structure " + locale);
                check(RequirementNames.resolve(true, entry.id(), "@missing.translation").equals("example:stronghold"), "missing translation fallback");
                check(RequirementNames.resolve(false, ResourceLocation.tryParse("minecraft:plains"), "高峰出云").equals("高峰出云"), "riddle remains literal");
                check(RequirementNames.resolve(false, ResourceLocation.tryParse("minecraft:deep_ocean"), "暖水深海（使用深海实现）").equals(translations.get("biome.the_long_travail.warm_deep_ocean")), "legacy custom biome translation " + locale);
                for (var aspect : TravailAspect.values()) for (boolean witness : new boolean[]{false, true}) {
                    String key = "gui.the_long_travail.diary.prose." + aspect.id() + (witness ? ".witness" : ".malice");
                    check(DiaryPageProse.text(aspect, witness).equals(translations.get(key)), "prose after language switch " + key);
                }
            }
        } finally { Language.inject(previous); }
        System.out.println("PASS: 36 legacy aliases, configured IDs/weights, literal riddles, missing-key fallback, structure translations and 12 prose pages across zh/en/zh switching.");
    }
    private static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
    }
}
