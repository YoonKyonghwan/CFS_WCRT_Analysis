package org.cap.simulation.comparator;

import org.cap.model.Task;

public class ReleaseTimeComparator extends FIFOComparator {

    @Override
    public int compareTieWithSameQueueInsertTime(Task o1, Task o2) {
        if(o1.bodyReleaseTime < o2.bodyReleaseTime) {
            return -1;
        }
        else if(o1.bodyReleaseTime == o2.bodyReleaseTime) {
            if(o1.bodyReleaseTime == 0) {
                if (o1.initialPriority < o2.initialPriority) {
                    return -1;
                } else if (o1.initialPriority == o2.initialPriority) {
                    return 0;
                } else { // o1.initialPriority > o2.initialPriority
                    return 1;
                }
            }
            else {
                return 0;
            }
        } else {
            return 1;
        }
    }
    
}
