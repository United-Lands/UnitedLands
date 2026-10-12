package org.unitedlands.unitedlands.utils;

import org.bukkit.plugin.java.JavaPlugin;
import org.unitedlands.utils.United;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;

public final class FontUtils {

    private static final int DEFAULT_WIDTH = 6; // fallback for unlisted characters
    private static final byte[] WIDTHS = new byte[65536]; // 0 = unknown

    public enum Align {
        LEFT, CENTER, RIGHT
    }

    public static void load(JavaPlugin plugin) {
        try (var in = plugin.getResource("font_widths.txt")) {
            if (in == null)
                throw new IllegalStateException("font_widths.txt missing");
            var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            int loaded = 0;
            while ((line = reader.readLine()) != null) {
                // Expected: "<char> length <n>"
                int idx = line.lastIndexOf(" length ");
                if (idx != 1)
                    continue; // char must be exactly one UTF-16 unit
                char c = line.charAt(0);
                WIDTHS[c] = Byte.parseByte(line.substring(idx + " length ".length()).trim());
                loaded++;
            }
            United.logger().info("Loaded " + loaded + " font widths");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static int width(char c, boolean bold) {
        int w = WIDTHS[c] == 0 ? DEFAULT_WIDTH : WIDTHS[c];
        return bold ? w + 1 : w;
    }

    public static int width(String s, boolean bold) {
        int total = 0;
        for (int i = 0; i < s.length(); i++)
            total += width(s.charAt(i), bold);
        return total;
    }

    /** Pixel width of an Adventure component tree, respecting bold inheritance. */
    public static int width(Component component) {
        return width(component, false);
    }

    private static int width(Component c, boolean parentBold) {
        TextDecoration.State state = c.style().decoration(TextDecoration.BOLD);
        boolean bold = state == TextDecoration.State.NOT_SET ? parentBold
                : state == TextDecoration.State.TRUE;
        int total = 0;
        if (c instanceof TextComponent t)
            total += width(t.content(), bold);
        for (Component child : c.children())
            total += width(child, bold);
        return total;
    }

    /**
     * Pads the component with invisible spaces so it is exactly {@code target}
     * pixels wide.
     * If the content is already wider than the target, it is returned unchanged.
     */
    public static Component padTo(Component content, int target, Align align) {
        int gap = target - width(content);
        if (gap <= 0)
            return content;

        int left, right;
        switch (align) {
            case LEFT -> {
                left = 0;
                right = gap;
            }
            case RIGHT -> {
                left = gap;
                right = 0;
            }
            default -> {
                left = gap / 2;
                right = gap - left;
                // Look for a split where both sides are exactly buildable
                for (int d = 0; d <= gap / 2; d++) {
                    if (reachable(gap / 2 - d) && reachable(gap - (gap / 2 - d))) {
                        left = gap / 2 - d;
                        right = gap - left;
                        break;
                    }
                    if (reachable(gap / 2 + d) && reachable(gap - (gap / 2 + d))) {
                        left = gap / 2 + d;
                        right = gap - left;
                        break;
                    }
                }
            }
        }

        return Component.text()
                .append(spaces(left))
                .append(content)
                .append(spaces(right))
                .build();
    }

    /** Can this many pixels be built from 4 px spaces and 5 px bold spaces? */
    private static boolean reachable(int px) {
        return px >= 0 && 5 * (px % 4) <= px;
    }

    private static Component spaces(int px) {
        while (!reachable(px))
            px--; // only matters for the unreachable widths below
        if (px <= 0)
            return Component.empty();

        int bold = px % 4; // each bold space is 1 px wider than a normal one
        int normal = (px - 5 * bold) / 4;

        var builder = Component.text();
        if (normal > 0)
            builder.append(Component.text(" ".repeat(normal)).decoration(TextDecoration.BOLD, false));
        if (bold > 0)
            builder.append(Component.text(" ".repeat(bold)).decorate(TextDecoration.BOLD));
        return builder.build();
    }
}