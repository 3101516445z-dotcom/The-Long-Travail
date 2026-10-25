package com.thelongtravail.item;

import com.thelongtravail.AspectTheme;

import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;

/** 仅持久化样式选择，不保存渲染后或翻译后的名称。 */
public final class NameColoring {
    public static final String KEY = "the_long_travail:name_style";
    public static final int CLEAR = 9;
    public static final int COUNT = 10;
    private static final java.util.regex.Pattern COLOR_CODES =
            java.util.regex.Pattern.compile("(?i)§[0-9a-fr]");
    private static final String[] IDS = {
        "travail", "flourishing", "abyss", "far_reach", "deep_valley",
        "underworld", "boundless", "prose", "flavor", "clear"
    };

    public static int selection(ItemStack stack) {
        var tag = stack.getTag();
        if (tag == null || !tag.contains(KEY, Tag.TAG_STRING)) return -1;
        String id = tag.getString(KEY);
        for (int i = 0; i < IDS.length; i++) if (IDS[i].equals(id)) return i;
        return -1;
    }

    public static boolean valid(int choice) {
        return choice >= 0 && choice < COUNT;
    }

    public static void apply(ItemStack stack, int choice) {
        if (!stack.isEmpty() && valid(choice)) stack.getOrCreateTag().putString(KEY, IDS[choice]);
    }

    public static Component label(int choice) {
        return Component.translatable("gui.the_long_travail.coloring.option." + IDS[choice]);
    }

    public static Component decorate(ItemStack stack, Component name) {
        int choice = selection(stack);
        return choice < 0
                ? name
                : transform(name, choice, stack.getRarity().color.getColor(), null);
    }

    /** 神化组装完整名称后，还原样式时保留品质颜色。 */
    public static Component clearAffix(Component name, int color) {
        return transform(name, CLEAR, color, color);
    }

    public static Component previewLabel(int choice) {
        return transform(label(choice), choice, 0xDDD3B5, null);
    }

    private static MutableComponent transform(
            Component source, int choice, Integer fallback, Integer forceClear) {
        var contents = source.getContents();
        if (contents instanceof TranslatableContents translation) {
            Object[] args = translation.getArgs().clone();
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Component component)
                    args[i] = transform(component, choice, fallback, forceClear);
                else if (args[i] instanceof String text && choice != CLEAR)
                    args[i] = cleanCodes(text);
            }
            contents =
                    new TranslatableContents(translation.getKey(), translation.getFallback(), args);
        } else if (contents instanceof LiteralContents literal && choice != CLEAR) {
            contents = new LiteralContents(cleanCodes(literal.text()));
        }
        MutableComponent result = MutableComponent.create(contents);
        Style style = source.getStyle();
        String marker = style.getInsertion();
        if (choice == CLEAR) {
            boolean ours =
                    marker != null
                            && (marker.equals("the_long_travail:name")
                                    || marker.startsWith("the_long_travail:aspect_name:")
                                    || marker.startsWith("the_long_travail:prose:")
                                    || marker.startsWith("the_long_travail:apotheosis:"));
            if (ours) style = style.withInsertion(null).withColor(fallback);
            if (forceClear != null) style = style.withColor(forceClear);
        } else if (choice == 0) {
            style = style.withColor(0xC5AA77).withInsertion("the_long_travail:name");
        } else if (choice <= 6) {
            style =
                    AspectTheme.values()[choice - 1]
                            .name(Component.empty())
                            .getStyle()
                            .applyTo(style);
        } else {
            style =
                    style.withColor(choice == 7 ? 0xBC995E : 0xC4AD75)
                            .withInsertion(
                                    choice == 7
                                            ? "the_long_travail:prose:coloring:auto"
                                            : "the_long_travail:prose:flavor:coloring:auto");
        }
        result.setStyle(style);
        for (Component sibling : source.getSiblings())
            result.append(transform(sibling, choice, fallback, forceClear));
        return result;
    }

    private static String cleanCodes(String text) {
        return text.indexOf('§') < 0 ? text : COLOR_CODES.matcher(text).replaceAll("");
    }

    private NameColoring() {}
}
