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
    }

    public void analyze() {
        for (Core core : this.cores) {
            for (Task task : core.tasks) {
                computeWCRTwithIntraCoreInterference(task, core);
            }
        }
    }

    /* 
    * Algorithm1 in the paper.
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

        // find the candidate of tau_k
        // List<Integer> candidate_k_index = new ArrayList<>(tasks.size());
        // for (Task task_k : tasks) {
        //     if ((task_i.id != task_k.id) && checkVruntimeSpeedCondition(task_i, task_k)) {
        //         candidate_k_index.add(task_k.id);
        //     }
        // }
        // if (candidate_k_index.size() == 0) {
        //     //task_i is the minimum vruntime (v_j^t >= v_i^t)
        // }else if (candidate_k_index.size() == 1 && candidate_k_index.get(0) == task_j.id) {
        //     v_j_gap += Math.max(getAlpha(task_i, w_j), (this.targetLatency / 2));
        // }else {
        //     double minWeight = 88761; //max_weight = 88761
        //     for (Task task_k: tasks) {
        //         if (candidate_k_index.contains(task_k.id) && task_k.weight < minWeight) {
        //             minWeight = task_k.weight;
        //         }
        //     }
        //     v_j_gap += getAlpha(task_i, minWeight);
        //     v_j_gap += this.targetLatency / 2;
        // }
        
        if (checkVruntimeSpeedCondition(task_i, task_j)){
            if (tasks.size() > 2) {
                double minWeight = 88761; //max_weight = 88761
                for (Task task: tasks) {
                    if (task.id != task_i.id && task.id != task_i.id && task.weight < minWeight) {
                        minWeight = task.weight;
                    }
                }
    
                v_j_gap += getAlpha(task_i, minWeight);
                v_j_gap += this.targetLatency / 2;
            }else{
                //because v_min^t is from one of the two tasks
                v_j_gap += Math.max(getAlpha(task_i, w_j), (this.targetLatency / 2));
            }
        }

        v_j_gap += C_i * ((double) NiceToWeight.getWeight(0) / w_i);
        v_j_gap += getGamma(task_i, task_j);
        long interference = (long) (v_j_gap * (w_j / NiceToWeight.getWeight(0)));

        // interference bound by workload
        int num_of_jobs = 0;
        long total_workload_j = 0;
        num_of_jobs = (int) Math.ceil((double) R_prev / T_j) + 1;
        total_workload_j = num_of_jobs * C_j;
        interference = Math.min(interference, total_workload_j);
        // if (R_prev > C_j){
        //     total_workload_j = C_j + ((R_prev-C_j) / T_j * C_j) + Math.min((R_prev-C_j) % T_j, C_j);
        //     interference = Math.min(interference, total_workload_j);        
        // }

        // interference bound by max busy interval
        num_of_jobs = (int) Math.ceil((double) maxBusyInterval / T_j);
        total_workload_j = num_of_jobs * C_j;
        interference = Math.min(interference, total_workload_j);        

        return interference;
    }


    private boolean checkVruntimeSpeedCondition(Task task_i, Task task_k){
        double C_i = task_i.bodyTime;
        double C_k = task_k.bodyTime;
        long T_i = task_i.period;
        long T_k = task_k.period;
        double w_i = task_i.weight;
        double w_k = task_k.weight;

        long v_i_speed = (long) (C_i / w_i);
        int num_job_k = (int) (T_i / T_k); //include floor
        long v_k_speed = (long) (num_job_k * C_k / w_k);
        return v_i_speed >= v_k_speed;
    }

    // private long getInterference(Task task_i, Task task_j, long R_prev, long maxBusyInterval, List<Task> tasks) {
    //     long T_j = task_j.period;
    //     int C_i = (int) task_i.bodyTime;
    //     int C_j = (int) task_j.bodyTime;
    //     double w_i = task_i.weight;
    //     double w_j = task_j.weight;

    //     // // interference bound by vruntime
    //     double alpha_j = getAlpha(tasks, task_j);
    //     double beta_i = getBeta(task_i);
    //     double gamma_ij = getGamma(task_i, task_j);

    //     // (\alpha_j +\beta_i + \gamma_{i,j} + \frac{L}{2}) \cdot \frac{w_j}{w_0} + C_i \cdot \frac{w_j}{w_i}
    //     double weight_ratio = w_j / (double) NiceToWeight.getWeight(0);
    //     // long interference = (long) ((alpha_j + beta_i + gamma_ij + this.targetLatency / 2) * weight_ratio);
    //     long interference = (long) ((beta_i + gamma_ij) * weight_ratio);
    //     interference += (long) this.targetLatency / 2 * weight_ratio;
    //     // interference += Math.max((long) alpha_j, this.targetLatency / 2) * weight_ratio;
    //     interference += (long) (C_i * (w_j / w_i));

    //     // interference bound by workload
    //     int num_of_jobs = (int) Math.ceil((double) R_prev / T_j) + 1;
    //     long total_workload_j = num_of_jobs * C_j;
    //     interference = Math.min(interference, total_workload_j);

    //     // interference bound by max busy interval
    //     num_of_jobs = (int) Math.ceil((double) maxBusyInterval / T_j);
    //     total_workload_j = num_of_jobs * C_j;
    //     interference = Math.min(interference, total_workload_j);        

    //     return interference;
    // }


    private double getAlpha(Task task_i, double minWeight) {
        double weight_ratio = (double) task_i.weight / (double) (task_i.weight + minWeight);
        double maxDelta = Math.max(this.targetLatency * weight_ratio, this.min_granularity);
        maxDelta = adjustTimeScliceWithJiffy(maxDelta);
        maxDelta = Math.min(maxDelta, task_i.bodyTime);
        double w_0 = NiceToWeight.getWeight(0);
        return maxDelta * (w_0 / task_i.weight);
    }

    private double getGamma(Task task_i, Task task_j) {
        double weight_ratio = (double) task_i.weight / (double) (task_i.weight + task_j.weight);
        double delta = Math.max(this.targetLatency * weight_ratio, this.min_granularity);
        delta = adjustTimeScliceWithJiffy(delta);
        delta = Math.min(delta, task_j.bodyTime);
        return (delta * NiceToWeight.getWeight(0)) / task_j.weight;
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
