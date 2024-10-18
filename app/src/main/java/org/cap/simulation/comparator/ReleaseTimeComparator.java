package org.cap.simulation.comparator;

import org.cap.model.TaskStat;

public class ReleaseTimeComparator implements TaskStatComparator {

    @Override
    public int compare(TaskStat o1, TaskStat o2) {
        if(o1.bodyReleaseTime < o2.bodyReleaseTime) {
            return -1;
        } else if(o1.bodyReleaseTime == o2.bodyReleaseTime) {
            return 0;
        } else {
            return 1;
        }
    }

}
