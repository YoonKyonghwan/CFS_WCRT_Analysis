package org.cap.simulation.comparator;

import org.cap.model.TaskStat;

public class TargetTaskBasedComparator extends FIFOComparator {

    boolean reverseOrder;

    public TargetTaskBasedComparator() {
        this.reverseOrder = false;
    }

    public TargetTaskBasedComparator(boolean reverseOrder) {
        this.reverseOrder = reverseOrder;
    }

    public int compareNotTargetTask(TaskStat o1, TaskStat o2) {
        return 0;
    }

    @Override
    public int compareTieWithOtherCases(TaskStat o1, TaskStat o2) {
    if(o1.task.isTargetTask == true && o2.task.isTargetTask == false) {
            return 1;
        }
        else if(o1.task.isTargetTask == false && o2.task.isTargetTask == true) {
            return -1;
        }
        else {
            if(this.reverseOrder == true) {
                return -compareNotTargetTask(o1, o2);
            } else {
                return compareNotTargetTask(o1, o2);
            }
            
        }
    }
}
