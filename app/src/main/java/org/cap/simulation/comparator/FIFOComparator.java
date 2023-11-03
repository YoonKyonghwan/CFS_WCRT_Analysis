package org.cap.simulation.comparator;

import org.cap.model.Task;

public class FIFOComparator implements BasicTaskComparator {

    public int compareTieWithOtherCases(Task o1, Task o2) {
        return 0;
    }

    public int compareTieWithInitialPriority(Task o1, Task o2) {
        if (o1.initialPriority < o2.initialPriority) {
            return -1;
        } else if (o1.initialPriority == o2.initialPriority) {
            return compareTieWithOtherCases(o1, o2);
        } else { // o1.initialPriority > o2.initialPriority
            return 1;
        }
    }
    public int compareTieWithSameQueueInsertTime(Task o1, Task o2) {
        if(o1.bodyReleaseTime == 0 && o2.bodyReleaseTime == 0) {
            return compareTieWithInitialPriority(o1, o2);
        }
        else {
            return compareTieWithOtherCases(o1, o2);
        }
    }

    @Override
    public int compareTie(Task o1, Task o2) {
        if(o1.getQueueInsertTime() < o2.getQueueInsertTime()) {
            return -1;
        }
        else if(o1.getQueueInsertTime() == o2.getQueueInsertTime()) {
            return compareTieWithSameQueueInsertTime(o1, o2);
        } else {
            return 1;
        }
    }
    
}
