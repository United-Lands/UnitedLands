package org.unitedlands.unitedlands.utils;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/**
 * Converts legacy color/format codes (&a, §a, &#RRGGBB, &x&R&R&G&G&B&B, §x§R§R§G§G§B§B)
 * into MiniMessage tags, while leaving existing MiniMessage tags untouched.
 * Works for legacy-only, MiniMessage-only and mixed input.
 */
public final class LegacyFormatUtils {

    private static final Map<Character, String> COLORS = Map.ofEntries(
            Map.entry('0', "black"),
            Map.entry('1', "dark_blue"),
            Map.entry('2', "dark_green"),
            Map.entry('3', "dark_aqua"),
            Map.entry('4', "dark_red"),
            Map.entry('5', "dark_purple"),
            Map.entry('6', "gold"),
            Map.entry('7', "gray"),
            Map.entry('8', "dark_gray"),
            Map.entry('9', "blue"),
            Map.entry('a', "green"),
            Map.entry('b', "aqua"),
            Map.entry('c', "red"),
            Map.entry('d', "light_purple"),
            Map.entry('e', "yellow"),
            Map.entry('f', "white")
    );

    private static final Map<Character, String> DECORATIONS = Map.of(
            'k', "obfuscated",
            'l', "bold",
            'm', "strikethrough",
            'n', "underlined",
            'o', "italic"
    );

    private LegacyFormatUtils() {}

    public static String convert(String input) {
        if (input == null || input.isEmpty()) return input;
        // Fast path: nothing legacy in here
        if (input.indexOf('&') == -1 && input.indexOf('§') == -1) return input;

        StringBuilder out = new StringBuilder(input.length() + 16);
        // Decorations opened by legacy codes, in opening order
        Deque<String> openDecorations = new ArrayDeque<>();

        int i = 0;
        final int n = input.length();

        while (i < n) {
            char c = input.charAt(i);

            // Escaped tag (\<) -> keep as-is so MiniMessage treats it as text
            if (c == '\\' && i + 1 < n && input.charAt(i + 1) == '<') {
                out.append(c).append('<');
                i += 2;
                continue;
            }

            // Existing MiniMessage tag -> copy verbatim (its contents may contain '&')
            if (c == '<') {
                int end = findTagEnd(input, i);
                if (end != -1) {
                    out.append(input, i, end + 1);
                    i = end + 1;
                    continue;
                }
            }

            if ((c == '&' || c == '§') && i + 1 < n) {
                char next = input.charAt(i + 1);

                // &#RRGGBB
                if (next == '#' && isHex(input, i + 2, 6)) {
                    closeDecorations(out, openDecorations);
                    out.append("<#").append(input, i + 2, i + 8).append('>');
                    i += 8;
                    continue;
                }

                // &x&R&R&G&G&B&B  (also §x§R§R§G§G§B§B)
                if ((next == 'x' || next == 'X') && i + 14 <= n) {
                    String hex = readBungeeHex(input, i + 2);
                    if (hex != null) {
                        closeDecorations(out, openDecorations);
                        out.append("<#").append(hex).append('>');
                        i += 14;
                        continue;
                    }
                }

                char code = Character.toLowerCase(next);

                String color = COLORS.get(code);
                if (color != null) {
                    // Legacy colors reset active decorations; MiniMessage colors don't
                    closeDecorations(out, openDecorations);
                    out.append('<').append(color).append('>');
                    i += 2;
                    continue;
                }

                String decoration = DECORATIONS.get(code);
                if (decoration != null) {
                    if (!openDecorations.contains(decoration)) {
                        openDecorations.addLast(decoration);
                        out.append('<').append(decoration).append('>');
                    }
                    i += 2;
                    continue;
                }

                if (code == 'r') {
                    out.append("<reset>");
                    openDecorations.clear();
                    i += 2;
                    continue;
                }
            }

            out.append(c);
            i++;
        }

        return out.toString();
    }

    /** Closes all legacy-opened decorations, most recent first. */
    private static void closeDecorations(StringBuilder out, Deque<String> open) {
        while (!open.isEmpty()) {
            out.append("</").append(open.removeLast()).append('>');
        }
    }

    /**
     * Reads "&R&R&G&G&B&B" starting at {@code start} (just after the 'x').
     * Returns the 6 hex digits or null if the pattern doesn't match.
     */
    private static String readBungeeHex(String s, int start) {
        StringBuilder hex = new StringBuilder(6);
        for (int k = 0; k < 6; k++) {
            int pos = start + k * 2;
            char sep = s.charAt(pos);
            if (sep != '&' && sep != '§') return null;
            char digit = s.charAt(pos + 1);
            if (Character.digit(digit, 16) == -1) return null;
            hex.append(digit);
        }
        return hex.toString();
    }

    private static boolean isHex(String s, int start, int length) {
        if (start + length > s.length()) return false;
        for (int k = start; k < start + length; k++) {
            if (Character.digit(s.charAt(k), 16) == -1) return false;
        }
        return true;
    }

    /**
     * Finds the closing '>' of a MiniMessage tag starting at {@code start} ('<'),
     * respecting quoted arguments. Returns -1 if this doesn't look like a tag.
     */
    private static int findTagEnd(String s, int start) {
        int n = s.length();
        if (start + 1 >= n) return -1;

        char first = s.charAt(start + 1);
        if (!(Character.isLetter(first) || first == '/' || first == '!' || first == '#')) return -1;

        char quote = 0;
        for (int j = start + 1; j < n; j++) {
            char ch = s.charAt(j);
            if (quote != 0) {
                if (ch == '\\' && j + 1 < n) { j++; continue; }
                if (ch == quote) quote = 0;
            } else if (ch == '\'' || ch == '"') {
                quote = ch;
            } else if (ch == '>') {
                return j;
            } else if (ch == '<') {
                return -1; // new tag opened before this one closed -> not a tag
            }
        }
        return -1;
    }
}