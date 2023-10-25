package org.cap.simulation.comparator;

import java.util.Comparator;

import org.cap.model.Task;

public interface BasicTaskComparator extends Comparator<Task> {

    default public int compareTie(Task o1, Task o2) {
        return 0;
    }

    @Override
    default public int compare(Task o1, Task o2) {
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
