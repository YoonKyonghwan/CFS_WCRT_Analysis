package org.cap.utility;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class CustomFormatter extends Formatter {
    @Override
    public String format(LogRecord record) {
        ArrayList<Object> parameters = new ArrayList<>();
        if(record.getParameters() != null) {
            for (Object elem : record.getParameters()) {
                parameters.add(elem.toString());
            }
        }
        return MessageFormat.format(record.getMessage(), parameters.toArray()) + "\n";
    }
}