package org.cap.simulation.comparator;

import java.util.Comparator;

import org.cap.model.TaskStat;

public interface BasicTaskComparator extends Comparator<TaskStat> {

    default public int compareTie(TaskStat o1, TaskStat o2) {
        return 0;
    }

    @Override
    default public int compare(TaskStat o1, TaskStat o2) {
        if (o1.virtualRuntime > o2.virtualRuntime) {
            return 1;
        }
        else if(o1.virtualRuntime == o2.virtualRuntime) {
            return compareTie(o1, o2);
        }
        else  { // o1.virtualRuntime < o2.virtualRuntime
            return -1;
        }
    }
}
