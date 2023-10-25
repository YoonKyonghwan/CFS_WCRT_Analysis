package org.cap.simulation.comparator;

import org.cap.model.Task;

public class PeriodComparator extends TargetTaskBasedComparator {
    @Override
    public int compareNotTargetTask(Task o1, Task o2) {
        if(o1.period < o2.period) { // short period executed earlier
            return -1;
        }
        else if(o1.period > o2.period) {
            return 1;
        }
        else {
            return 0;
        }
    }
}
