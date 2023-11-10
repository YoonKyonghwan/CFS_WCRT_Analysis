package org.cap.simulation.comparator;

import org.cap.model.TaskStat;

public class PeriodComparator extends TargetTaskBasedComparator {
    @Override
    public int compareNotTargetTask(TaskStat o1, TaskStat o2) {
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
