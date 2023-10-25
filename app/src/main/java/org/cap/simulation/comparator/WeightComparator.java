package org.cap.simulation.comparator;

import org.cap.model.Task;

public class WeightComparator extends TargetTaskBasedComparator {
    @Override
    public int compareNotTargetTask(Task o1, Task o2) {
        if(o1.nice < o2.nice) { // smaller nice executed first
            return -1;
        }
        else if(o1.nice > o2.nice) {
            return 1;
        }
        else {
            return 0;
        }
    }
}
