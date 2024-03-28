package org.cap.simulation;

import java.util.List;
import org.cap.model.Core;
import org.cap.model.Task;

public class FIFOAnalyzer {
    private List<Core> cores;

    public FIFOAnalyzer(List<Core> cores) {
        this.cores = cores;
        for (Core core: this.cores) {
            // calculate WCRT for each core
            int wcrt_core = 0;
            for (Task task: core.tasks) {
                wcrt_core += task.bodyTime;
            }
            
            // check schedulability
            for (Task task: core.tasks) {
                if (task.period < wcrt_core) {
                    task.isSchedulable_by_FIFO = false;
                } else {
                    task.isSchedulable_by_FIFO = true;
                }
            }
        }
    }
}
