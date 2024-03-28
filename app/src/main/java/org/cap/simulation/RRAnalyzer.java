package org.cap.simulation;

import java.util.List;
import org.cap.model.Core;
import org.cap.model.Task;

public class RRAnalyzer {
    private List<Core> cores;
    int time_slice;
    private int jiffy_us = 1000; // 1ms

    public RRAnalyzer(List<Core> cores, int time_slice) {
        this.cores = cores;
        this.time_slice = adjustTimeScliceWithJiffy(time_slice);

        for (Core core: this.cores) {
            // calculate WCRT for each core
            for (Task task_i: core.tasks) {
                int requiredNumOfTimeSlices = (int) Math.ceil(task_i.bodyTime / this.time_slice);
                long wcrt = (long) task_i.bodyTime;
                for (Task task_j: core.tasks) {
                    if (task_i.id != task_j.id) {
                        int C_j = (int) task_j.bodyTime;
                        wcrt += Math.min(requiredNumOfTimeSlices * this.time_slice, C_j);
                    }
                }

                if (task_i.period < wcrt) {
                    task_i.isSchedulable_by_RR = false;
                } else {
                    task_i.isSchedulable_by_RR = true;
                }
            }
        }
    }

    private int adjustTimeScliceWithJiffy(int time_slice){
        return (Math.floorDiv(time_slice, this.jiffy_us) + 1) * this.jiffy_us;
    }

    public boolean checkSchedulability() {
        for (Core core: this.cores) {
            for (Task task: core.tasks) {
                if (!task.isSchedulable_by_RR) {
                    return false;
                }
            }
        }
        return true;
    }
}
