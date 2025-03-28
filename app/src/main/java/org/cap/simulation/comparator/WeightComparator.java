package org.cap.simulation.comparator;

import org.cap.model.TaskStat;

public class WeightComparator implements TaskStatComparator {

    @Override
    public int compare(TaskStat o1, TaskStat o2) {
        if(o1.task.nice < o2.task.nice) { // smaller nice executed first
            return -1;
        }
        else if(o1.task.nice > o2.task.nice) {
            return 1;
        }
        else {
            return 0;
        }
    }

}
