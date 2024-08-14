package org.cap.simulation;

import java.util.List;
import org.cap.model.Core;
import org.cap.model.NiceToWeight;
import org.cap.model.Task;

public class CFSAnalyzer_v2 {
    private List<Core> cores;
    private int targetLatency; 
    private int min_granularity;
    private int jiffy_us; // default 1ms

    public CFSAnalyzer_v2(List<Core> cores, int targetLatency, int min_granularity, int jiffy_us) {
        this.cores = cores;
        this.targetLatency = targetLatency;
        this.min_granularity = min_granularity;
        this.jiffy_us = jiffy_us;

        double minWeight = 88761; //max_weight = 88761
        for (Core core: this.cores) {
            for (Task task: core.tasks) {
                task.weight = NiceToWeight.getWeight(task.nice);
                if (task.weight < minWeight) {
                    minWeight = task.weight;
                }
                // task.period = task.period/1000; // ns -> us
            }
            core.minWeight = minWeight;
        }
    }

    public void analyze() {
        for (Core core : this.cores) {
            for (Task task : core.tasks) {
                computeWCRTwithIntraCoreInterference(task, core);
            }
        }
    }

    /* 
    * Theorem1 in the paper.
    */
    private void computeWCRTwithIntraCoreInterference(Task task_i, Core core) {
        //for test
        long maxBusyInterval = getMaxBusyInterval(core, task_i);
        
        int C_i = (int) task_i.bodyTime;
        long R_prev = C_i;
        boolean convergence = false;
        while(!convergence){
            long R_cur = C_i;
            for (Task task_j : core.tasks) {
                if (task_i.id != task_j.id) {
                    double minWeight = checkMinWeight(core, task_i);
                    R_cur += getInterference(task_i, task_j, R_prev, minWeight, maxBusyInterval);
                }
            }
            if (task_i.period < R_cur) {
                task_i.WCRT_by_proposed = R_cur;
                task_i.isSchedulable_by_proposed = false;
                convergence = true;
            }else{
                if (R_prev == R_cur){
                    task_i.WCRT_by_proposed = R_cur;
                    task_i.isSchedulable_by_proposed = true;
                    convergence = true;
                }else{
                    R_prev = R_cur;
                }
            }
        }
        return;
    }

    private double checkMinWeight(Core core, Task task_i) {
        double minWeight = 88761; //max_weight = 88761
        for (Task task: core.tasks) {
            if (task.id != task_i.id && task.weight < minWeight) {
                minWeight = task.weight;
            }
        }
        return minWeight;
    }

    /*
     * Theorem1 in the paper.
     */
    private long getInterference(Task task_i, Task task_j, long R_prev, double minWeight, long maxBusyInterval) {
        long T_j = task_j.period;
        int C_i = (int) task_i.bodyTime;
        int C_j = (int) task_j.bodyTime;
        double w_i = task_i.weight;
        double w_j = task_j.weight;

        double alpha = Math.max(this.targetLatency * w_i / (w_i + minWeight), this.min_granularity);
        alpha = adjustTimeScliceWithJiffy(alpha);
        double gamma = Math.max(this.targetLatency * w_j / (w_i + w_j), this.min_granularity);
        gamma = adjustTimeScliceWithJiffy(gamma);

        // interference bound by vruntime
        long interference = (long)(alpha*(w_j / w_i));
        interference += (long)(C_i*(w_j / w_i));
        interference += gamma;

        // interference bound by workload
        // total_workload = (ceil(R_prev / T_j)+1) * C_j
        int num_of_jobs = (int) Math.ceil((double) R_prev / T_j) + 1;
        long total_workload_j = num_of_jobs * C_j;
        interference = Math.min(interference, total_workload_j);

        // interference bound by max busy interval
        num_of_jobs = (int) Math.ceil((double) maxBusyInterval / T_j);
        total_workload_j = num_of_jobs * C_j;
        interference = Math.min(interference, total_workload_j);        

        return interference;
    }

    /*
     * Definition 3 in the paper.
     */
    private double adjustTimeScliceWithJiffy(double time_slice){
        return (Math.floor(time_slice / this.jiffy_us) + 1) * this.jiffy_us;
    }


    public boolean checkSystemSchedulability() {
        for (Core core : this.cores) {
            for (Task task : core.tasks) {
                if (!task.isSchedulable_by_proposed) {
                    return false;
                }
            }
        }
        return true;
    }

    public long getMaxBusyInterval(Core core, Task task_i) {
        int C_i = (int) task_i.bodyTime;

        long maxBusyInterval_prev = C_i;
        while(true){
            long maxBusyInterval_cur = C_i;
            for (Task task_j : core.tasks) {
                if (task_i.id != task_j.id) {
                    int C_j = (int) task_j.bodyTime;
                    long total_workload_j = (long) Math.ceil((double) maxBusyInterval_prev / task_j.period) * C_j;
                    maxBusyInterval_cur += total_workload_j;
                }
            }
            
            if (maxBusyInterval_prev == maxBusyInterval_cur){
                return maxBusyInterval_cur;
            }else{
                maxBusyInterval_prev = maxBusyInterval_cur;
            }
        }
    }
}
