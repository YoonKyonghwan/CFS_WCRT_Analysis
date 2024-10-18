package org.cap.utility;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoggerUtility {
    private static final Logger logger = Logger.getLogger(LoggerUtility.class.getName());

    public static void initializeLogger(String logger_option) {
        try {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
            String formatDateTime = now.format(formatter);

            FileHandler fileHandler = new FileHandler("./logs/simulation_" + formatDateTime + ".txt");
            fileHandler.setFormatter(new CustomFormatter());
            logger.setUseParentHandlers(false);
            logger.addHandler(fileHandler);
            switch (logger_option) { // logger_option = ["off", "info", "fine"]
                case "off":
                    logger.setLevel(Level.OFF);
                    break;
                case "info":
                    logger.setLevel(Level.INFO);
                    break;
                case "fine":
                    logger.setLevel(Level.FINE);
                    break;
                default:
                    break;
            }
            
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
