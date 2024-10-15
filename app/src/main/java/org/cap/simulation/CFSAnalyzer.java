package org.cap.simulation;

import java.util.List;
import org.cap.model.Core;
import org.cap.model.NiceToWeight;
import org.cap.model.Task;


public class CFSAnalyzer {
    private List<Core> cores;
    private int targetLatency; 
    private int min_granularity;
    private int jiffy_us; // default 1ms


    public CFSAnalyzer(List<Core> cores, int targetLatency, int min_granularity, int jiffy_us) {
        this.cores = cores;
        this.targetLatency = targetLatency;
        this.min_granularity = min_granularity;
        this.jiffy_us = jiffy_us;
    }


    public void analyze() {
        for (Core core : this.cores) {
            for (Task task : core.tasks) {
                computeWCRTwithIntraCoreInterference(task, core);
            }
        }
    }


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
                    R_cur += getInterference(task_i, task_j, R_prev, maxBusyInterval, core.tasks);
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


    private long getInterference(Task task_i, Task task_j, long R_prev, long maxBusyInterval, List<Task> tasks) {
        long T_j = task_j.period;
        int C_i = (int) task_i.bodyTime;
        int C_j = (int) task_j.bodyTime;
        double w_i = task_i.weight;
        double w_j = task_j.weight;

        // // interference bound by vruntime
        double v_j_gap = 0;        
        if (tasks.size() > 2) {
            double weight_k = 88761; //max_weight = 88761 (NiceToWeight.getWeight(-20))
            for (Task task: tasks) {
                if (task.id != task_i.id && task.id != task_j.id && task.weight < weight_k) {
                    weight_k = task.weight;
                }
            }
            // if tau_k is V_min^t
            double candidate1 = getAlpha(task_i, weight_k) + this.targetLatency / 2;
            // if tau_j is V_min^t
            double candidate2 = getAlpha(task_i, w_j);
            // if tau_i is V_min^t, then v_j_gap = this.targetLatency / 2 and always smaller than candidate1
            v_j_gap += Math.max(candidate1, candidate2);
        }else{
            //because v_min^t is from one of the two tasks
            double candidate1 = getAlpha(task_i, w_j);
            double candidate2 = (this.targetLatency / 2);
            v_j_gap += Math.max(candidate1, candidate2);
        }

        v_j_gap += getGamma(task_i, task_j);
        double w_0 = NiceToWeight.getWeight(0);
        long interference = (long) (v_j_gap * (w_j / w_0));
        interference += (long) (C_i * (w_j / w_i));

        // interference bound by workload
        int num_of_jobs = 0;
        long total_workload_j = 0;
        num_of_jobs = (int) Math.ceil((double) R_prev / T_j) + 1;
        total_workload_j = num_of_jobs * C_j;
        interference = Math.min(interference, total_workload_j);

        // interference bound by max busy interval
        num_of_jobs = (int) Math.ceil((double) maxBusyInterval / T_j);
        total_workload_j = num_of_jobs * C_j;
        interference = Math.min(interference, total_workload_j);        

        return interference;
    }


    private double getAlpha(Task task_i, double minWeight) {
        double weight_ratio = (double) task_i.weight / (double) (task_i.weight + minWeight);
        double maxDelta = Math.max(this.targetLatency * weight_ratio, this.min_granularity);
        maxDelta = adjustTimeScliceWithJiffy(maxDelta);
        maxDelta = Math.min(maxDelta, task_i.bodyTime);
        double w_0 = NiceToWeight.getWeight(0);
        double alpha = maxDelta * w_0 / task_i.weight;
        return alpha;
    }


    private double getGamma(Task task_i, Task task_j) {
        double weight_ratio = (double) task_j.weight / (double) (task_i.weight + task_j.weight);
        double delta = Math.max(this.targetLatency * weight_ratio, this.min_granularity);
        delta = adjustTimeScliceWithJiffy(delta);
        delta = Math.min(delta, task_j.bodyTime);
        double w_0 = NiceToWeight.getWeight(0);
        double gamma = delta * w_0 / task_j.weight;
        return gamma;
    }


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
