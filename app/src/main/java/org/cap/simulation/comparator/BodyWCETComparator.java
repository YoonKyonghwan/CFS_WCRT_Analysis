package org.cap.simulation.comparator;

import org.cap.model.Task;

public class BodyWCETComparator extends TargetTaskBasedComparator {
    @Override
    public int compareNotTargetTask(Task o1, Task o2) {
        if(o1.bodyTime > o2.bodyTime) { // long body time executed earlier
            return -1;
        }
        else if(o1.bodyTime < o2.bodyTime) {
            return 1;
        }
        else {
            return 0;
        }
    }
}
