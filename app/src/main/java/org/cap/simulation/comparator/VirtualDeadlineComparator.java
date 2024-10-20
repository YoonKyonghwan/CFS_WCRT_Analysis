package org.cap.simulation.comparator;

import org.cap.model.TaskStat;

public class VirtualDeadlineComparator implements TaskStatComparator {
    @Override
    public int compare(TaskStat o1, TaskStat o2) {
        if (o1.virtualDeadline > o2.virtualDeadline) {
            return 1;
        }
        else if(o1.virtualDeadline == o2.virtualDeadline) {
            return 0;
        }
        else  { // o1.virtualDeadline < o2.virtualDeadline
            return -1;
        }
    }
}
