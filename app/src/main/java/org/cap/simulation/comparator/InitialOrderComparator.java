package org.cap.simulation.comparator;

import org.cap.model.TaskStat;

public class InitialOrderComparator implements TaskStatComparator {

    @Override
    public int compare(TaskStat o1, TaskStat o2) {
        if (o1.task.initialPriority < o2.task.initialPriority) {
            return -1;
        } else if (o1.task.initialPriority == o2.task.initialPriority) {
            return 0;
        } else { // o1.initialPriority > o2.initialPriority
            return 1;
        }
    }

}
