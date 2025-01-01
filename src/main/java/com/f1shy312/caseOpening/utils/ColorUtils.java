package com.f1shy312.caseOpening.utils;

import com.f1shy312.caseOpening.main;
import net.md_5.bungee.api.ChatColor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtils {
    private static final Pattern HEX_PATTERN = Pattern.compile("#[a-fA-F0-9]{6}");
    
    public static String colorize(String message) {
        if (message == null) return "";
        
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String color = matcher.group();
            matcher.appendReplacement(buffer, ChatColor.of(color).toString());
        }

        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    public static String formatMessage(main plugin, String message) {
        if (message == null) return "";
        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6Crates&8] ");
        return colorize(message.replace("%prefix%", prefix));
    }
} 