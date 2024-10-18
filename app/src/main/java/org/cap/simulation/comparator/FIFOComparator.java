package org.cap.simulation.comparator;

import org.cap.model.TaskStat;

public class FIFOComparator implements TaskStatComparator {

    @Override
    public int compare(TaskStat o1, TaskStat o2) {
        if(o1.getQueueInsertTime() < o2.getQueueInsertTime()) {
            return -1;
        }
        else if(o1.getQueueInsertTime() == o2.getQueueInsertTime()) {
            return 0;
        } else {
            return 1;
        }
    }
}
