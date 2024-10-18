package org.cap.simulation.comparator;

import org.cap.model.TaskStat;

public class PeriodComparator implements TaskStatComparator {

    @Override
    public int compare(TaskStat o1, TaskStat o2) {
        if(o1.task.period < o2.task.period) { // short period executed earlier
            return -1;
        }
        else if(o1.task.period > o2.task.period) {
            return 1;
        }
        else {
            return 0;
        }
    }

}
