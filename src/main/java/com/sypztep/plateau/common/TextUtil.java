package com.sypztep.plateau.common;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;

public final class TextUtil {
    public static List<String> wrapText(Font textRenderer, String text, int maxWidth) {
        List<String> lines = new ArrayList<>();

        String[] explicitLines = text.split("\\n"); // for spacing

        for (String line : explicitLines) {
            String[] words = line.split(" ");

            if (words.length == 0) {
                lines.add("");
                continue;
            }

            StringBuilder currentLine = new StringBuilder();

            for (String word : words) {
                String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;

                if (textRenderer.width(testLine) <= maxWidth) currentLine = new StringBuilder(testLine);
                else {
                    if (!currentLine.isEmpty()) {
                        lines.add(currentLine.toString());
                        currentLine = new StringBuilder(word);
                    } else lines.add(word);
                }
            }

            if (!currentLine.isEmpty()) lines.add(currentLine.toString());

        }

        return lines;
    }

    public static String warpLine(String text, int maxWidth, Font renderer) {
        if (renderer.width(text) <= maxWidth) {
            return text;
        }

        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (renderer.width(sb.toString() + c + "...") > maxWidth) {
                sb.append("...");
                break;
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
