package org.cap;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

class CustomFormatter extends Formatter {
    @Override
    public String format(LogRecord record) {
        return record.getMessage() + "\n";
    }
}