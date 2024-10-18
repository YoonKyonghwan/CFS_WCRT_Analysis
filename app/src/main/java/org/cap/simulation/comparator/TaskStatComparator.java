package org.cap.simulation.comparator;

import java.util.Comparator;

import org.cap.model.TaskStat;

public interface TaskStatComparator extends Comparator<TaskStat> {

    @Override
    abstract int compare(TaskStat o1, TaskStat o2);
}
