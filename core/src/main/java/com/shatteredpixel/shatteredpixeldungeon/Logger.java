package com.shatteredpixel.shatteredpixeldungeon;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {

    private static final String LOG_FILE = "mob_log.txt";

    private static final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    public static void log(String eventType, String message) {

        String timestamp = LocalDateTime.now().format(formatter);

        String line =
                "[" + timestamp + "] "
                        + "[" + eventType + "] "
                        + message;

        System.out.println(line);

        // append to log file
        try (PrintWriter writer =
                     new PrintWriter(new FileWriter(LOG_FILE, true))) {

            writer.println(line);

        } catch (IOException e) {
            System.err.println("Failed to write log file.");
        }
    }

    public static void logSpawn(String mobName, int mobId) {

        Logger.log(
                "SPAWN",
                "mob=" + mobName
                        + " id=" + mobId
        );
    }

    public static void logStateChange(
            String mobName,
            int mobId,
            String oldState,
            String newState
    ) {

        Logger.log(
                "STATE_CHANGE",
                "mob=" + mobName
                        + " id=" + mobId
                        + " from=" + oldState
                        + " to=" + newState
        );
    }

    public static void logAlert(
            String mobName,
            int mobId
    ) {

        Logger.log(
                "ALERT",
                "mob=" + mobName
                        + " id=" + mobId
        );
    }

    public static void logTargetChange(
            String mobName,
            int mobId,
            int oldTarget,
            int newTarget
    ) {
        String sOldTarget = Integer.toString(oldTarget);
        String sNewTarget = Integer.toString(newTarget);
        if (oldTarget == -1){
            sOldTarget = "No Target";
        } else if (newTarget == -1){
            sNewTarget = "No Target";
        }

        Logger.log(
                "TARGET_CHANGE",
                "mob=" + mobName
                        + " id=" + mobId
                        + " from=" + sOldTarget
                        + " to=" + sNewTarget
        );
    }
}