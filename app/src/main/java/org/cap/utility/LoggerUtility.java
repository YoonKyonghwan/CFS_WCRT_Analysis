package org.cap.utility;

import java.util.logging.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.IOException;

public class LoggerUtility {
    private static final Logger logger = Logger.getLogger(LoggerUtility.class.getName());

    public static void initializeLogger() {
        try {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
            String formatDateTime = now.format(formatter);

            FileHandler fileHandler = new FileHandler("./logs/simulation_" + formatDateTime + ".txt");
            fileHandler.setFormatter(new CustomFormatter());
            logger.setUseParentHandlers(false);
            logger.addHandler(fileHandler);
            logger.setLevel(Level.FINE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void addConsoleLogger() {
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new CustomFormatter());
        logger.addHandler(consoleHandler);
    }

    public static Logger getLogger() {
        return logger;
    }
}
