package org.cap.simulation.comparator;

import java.util.ArrayList;
import java.util.Comparator;

import org.cap.model.TaskStat;

public class MultiComparator implements Comparator<TaskStat> {

    ArrayList<TaskStatComparator> compareList;

    public MultiComparator() {
        this.compareList = new ArrayList<>();
    }

    public void insertComparator(TaskStatComparator compare) {
        this.compareList.add(compare);
    }

    @Override
    public int compare(TaskStat o1, TaskStat o2) {
        int ret = 0;
        for (TaskStatComparator compareClass : this.compareList) {
            ret = compareClass.compare(o1, o2);
            if (ret != 0) {
                break;
            }
        }

        return ret;
    }
}
