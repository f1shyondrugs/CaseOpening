package com.f1shy312.caseOpening.managers;

import com.f1shy312.caseOpening.main;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogManager {
    private final main plugin;
    private final File logFile;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public LogManager(main plugin) {
        this.plugin = plugin;
        this.logFile = new File(plugin.getDataFolder(), "purchases.log");
        createLogFileIfNotExists();
    }

    private void createLogFileIfNotExists() {
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create purchases.log file!");
                e.printStackTrace();
            }
        }
    }

    public void logKeyPurchase(String playerName, String keyType, int amount, double price) {
        String timestamp = LocalDateTime.now().format(formatter);
        String logMessage = String.format("[%s] %s purchased %dx %s key(s) for %.2f coins%n", 
            timestamp, playerName, amount, keyType, price);

        try (FileWriter fw = new FileWriter(logFile, true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(logMessage);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not write to purchases.log!");
            e.printStackTrace();
        }
    }
} 