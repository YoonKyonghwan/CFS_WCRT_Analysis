package org.cap.utility;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class CustomFormatter extends Formatter {
    @Override
    public String format(LogRecord record) {
        return record.getMessage() + "\n";
    }
}