package org.cap.simulation.comparator;

import org.cap.model.TaskStat;

public class BodyWCETComparator extends TargetTaskBasedComparator {
    @Override
    public int compareNotTargetTask(TaskStat o1, TaskStat o2) {
        if(o1.task.bodyTime > o2.task.bodyTime) { // long body time executed earlier
            return -1;
        }
        else if(o1.task.bodyTime < o2.task.bodyTime) {
            return 1;
        }
        else {
            return 0;
        }
    }
}
