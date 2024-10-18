package org.cap.simulation.comparator;

import org.cap.model.TaskStat;

public class VirtualRuntimeComparator implements TaskStatComparator {
    @Override
    public int compare(TaskStat o1, TaskStat o2) {
        if (o1.virtualRuntime > o2.virtualRuntime) {
            return 1;
        }
        else if(o1.virtualRuntime == o2.virtualRuntime) {
            return 0;
        }
        else  { // o1.virtualRuntime < o2.virtualRuntime
            return -1;
        }
    }
}
