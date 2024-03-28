package org.cap.simulation;

import java.util.List;
import org.cap.model.Core;
import org.cap.model.Task;

public class RMAnalyzer {
    private List<Core> cores;

    public RMAnalyzer(List<Core> cores) {
        this.cores = cores;
        for (Core core: this.cores) {
            for (Task task_i: core.tasks) {
                long C_i = (long) task_i.bodyTime;
                long prev_R = C_i;
                // calculate WCRT for each core
                while (true) {
                    long cur_R = C_i;
                    for (Task task_j: core.tasks) {
                        if (task_i.id != task_j.id) {
                            if (task_j.period >= task_i.period) {
                                cur_R += Math.ceil((double) prev_R / (double) task_j.period) * task_j.bodyTime;
                            }
                        }
                    }
                    
                    // check schedulability
                    if (task_i.period < cur_R) {
                        task_i.isSchedulable_by_RM = false;
                        break;
                    }else if (prev_R == cur_R){
                        task_i.isSchedulable_by_RM = true;
                        break;
                    }else{
                        prev_R = cur_R;
                    }
                }
            }
        }
    }
}
