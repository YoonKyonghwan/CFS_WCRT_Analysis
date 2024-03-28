package org.cap.simulation;

import java.util.List;
import org.cap.model.Core;
import org.cap.model.NiceToWeight;
import org.cap.model.Task;

public class CFSAnalyzer {
    private List<Core> cores;
    private int targetLatency; 
    private int min_granularity;
    private double w_0 = 1024.0;
    private int jiffy_us = 1000; // default 1ms

    public CFSAnalyzer(List<Core> cores, int targetLatency, int min_granularity, int jiffy_us) {
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
                task.period = task.period/1000; // ns -> us
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
        int C_i = (int) task_i.bodyTime;

        long R_prev = C_i;
        long R_cur = 0;
        while(true){
            R_cur = C_i;
            for (Task task_j : core.tasks) {
                if (task_i.id != task_j.id) {
                    R_cur += getInterference(task_j, R_prev, core, task_i);
                }
            }

            if (task_i.period < R_cur) {
                task_i.WCRT_by_proposed = R_cur;
                task_i.isSchedulable_by_proposed = false;
                break;
            }else if (R_prev == R_cur){
                task_i.WCRT_by_proposed = R_cur;
                task_i.isSchedulable_by_proposed = true;
                break;
            }else{
                R_prev = R_cur;
            }
        }
        return;
    }

    /*
     * Theorem1 in the paper.
     */
    private long getInterference(Task task_j, long R_prev, Core core, Task task_i) {
        long T_j = task_j.period;
        int C_j = (int) task_j.bodyTime;
        int C_i = (int) task_i.bodyTime;
        int k_j = (int) Math.floorDiv(R_prev, T_j);
        long interference = k_j * C_j;

        double w_i = task_i.weight;
        long lastRequestTime = k_j * T_j; // floor(R_prev / T_j) * T_j
        int remainWorkload = C_i;
        if (lastRequestTime > 0){
            remainWorkload = getRemainWorkload(lastRequestTime, task_i, core);
        }
        long S_j = 0;
        if(remainWorkload != 0){
            S_j = getInterferenceOfLastJob(task_j, remainWorkload, w_i, k_j, core.minWeight);
        }
        interference += S_j;

        return interference;
    }

    
    /*
     * Lemma6 in the paper.
     */
    private long getInterferenceOfLastJob(Task task_j, int remainWorkload, double w_i, int k_j, double minWeight) {
        int C_j = (int) task_j.bodyTime;
        double w_j = task_j.weight;
        int alpha = (int) Math.max(this.targetLatency * w_i / (w_i + minWeight), this.min_granularity);
        alpha = adjustTimeScliceWithJiffy(alpha);
        int gamma = (int) Math.max(this.targetLatency * w_j / (w_i + w_j), this.min_granularity);
        gamma = adjustTimeScliceWithJiffy(gamma);
        // if (k_j == 0) { // if the number of jobs of task_j is 1
        //     return (int) Math.min(remainWorkload * w_j / w_i + gamma, C_j);
        // }else{
        //     return (int) Math.min((alpha + remainWorkload) * w_j / w_i + gamma, C_j);
        // }
        return (int) Math.min((alpha + remainWorkload) * w_j / w_i + gamma, C_j);
        
    }


    /*
     * Corollary 3 in the paper.
     */
    private int getRemainWorkload(long lastRequestTime, Task task_i, Core core) {
        int C_i = (int) task_i.bodyTime;

        int usage_bound_1 = getUsageBound_1(lastRequestTime, task_i, core);
        int usage_bound_2 = getUsageBound_2(lastRequestTime, task_i, core);
        int final_usage_bound = Math.max(usage_bound_1, usage_bound_2);

        if(final_usage_bound > C_i){
            return 0;
        }else{
            return C_i - final_usage_bound;
        }
    }


    /*
     * Lemma 8~9 in the paper.
     */
    private int getUsageBound_2(long lastRequestTime, Task task_i, Core core){
        long total_workload = 0;
        for (Task task_j : core.tasks) {
            if (task_i.id != task_j.id) {
                long T_j = task_j.period;
                int C_j = (int) task_j.bodyTime;
                long psi = Math.floorDiv(lastRequestTime, T_j) * C_j;
                psi += Math.min(C_j, lastRequestTime % T_j);
                total_workload += psi;
            }
        }
        long eta = Math.min(total_workload, lastRequestTime);
        return (int) (lastRequestTime - eta);
    }


    /*
     * Lemma 7 in the paper.
     */
    private int getUsageBound_1(long lastRequestTime, Task task_i, Core core){        
        long T_x = 0;
        int C_x = 0;
        double w_x = 0;
        double w_i = task_i.weight;
        double max_objective = 0;
        // argmax (Equation 29)
        for (Task task_k : core.tasks) {
            if (task_i.id != task_k.id) {
                long T_y = task_k.period;
                int C_y = (int) task_k.bodyTime;
                double w_y = task_k.weight;
                double objective = Math.floorDiv(lastRequestTime, T_y) * C_y * (this.w_0 / w_y);
                if (objective > max_objective) {
                    max_objective = objective;
                    T_x = T_y;
                    C_x = C_y;
                    w_x = w_y;
                }
            }
        }

        int beta = Math.max((int) (this.targetLatency * (w_x / (w_i + w_x))), this.min_granularity);
        beta = adjustTimeScliceWithJiffy(beta);
        int zeta = (int) ((Math.floorDiv(lastRequestTime, T_x) * C_x - beta) * (w_i / w_x));
        return zeta;
    }

    /*
     * Definition 3 in the paper.
     */
    private int adjustTimeScliceWithJiffy(int time_slice){
        return (Math.floorDiv(time_slice, this.jiffy_us) + 1) * this.jiffy_us;
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
}
