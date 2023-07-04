package org.cap;

import java.util.Collections;
import java.util.List;

public class TaskWriter {

    public void writeResponseTimes(List<Task> tasks) {
        for (Task task : tasks) {
            int wcrt = Collections.max(task.responseTimes);
            System.out.println("Task with nice " + task.nice + " has WCRT " + wcrt);
        }
    }
}
