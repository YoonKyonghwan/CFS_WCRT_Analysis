package org.cap.utility;

import java.text.MessageFormat;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class CustomFormatter extends Formatter {
    @Override
    public String format(LogRecord record) {
        return MessageFormat.format(record.getMessage(), record.getParameters()) + "\n";
    }
}