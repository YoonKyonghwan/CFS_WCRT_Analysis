package org.cap;

import java.util.List;

public class TaskWriter {

    public void writeResponseTimes(List<Task> tasks) {
        System.out.println("-------- Worst-Case Response Times --------");
        for (Task task : tasks) {
            System.out.println("id " + task.id + " task has WCRT of " + task.worstCaseResponseTime + "s");
        }
    }
}
