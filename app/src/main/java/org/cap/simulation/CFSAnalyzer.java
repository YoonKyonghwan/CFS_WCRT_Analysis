package org.cap.simulation;

import java.util.List;
// import java.util.concurrent.ExecutorService;
// import java.util.concurrent.Executors;

import org.cap.model.Core;
import org.cap.model.NiceToWeight;
import org.cap.model.Task;

public class CFSAnalyzer {
    private List<Core> cores;
    private int targetLatency; 

    // private static final int max_num_threads = 8;
    public CFSAnalyzer(List<Core> cores, int targetLatency) {
        this.cores = cores;
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
        this.targetLatency = targetLatency;
    }

    public void analyze() {
        for (Core core : this.cores) {
            for (Task task : core.tasks) {
                computeWCRTwithIntraCoreInterference(task, core);
            }
        }
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


    /* 
    * Theorem1 in the paper.
    */
    private void computeWCRTwithIntraCoreInterference(Task task_i, Core core) {
        int C_i = (int) task_i.bodyTime;

        int R_prev = C_i;
        int R_cur = 0;
        while(true){
            R_cur = C_i;
            for (Task task_j : core.tasks) {
                if (task_i.id != task_j.id) {
                    long interference = getInterference(task_j, R_prev, core, task_i);
                    R_cur += interference;
                }
            }

            if (task_i.period < R_cur) {
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
     * Lemma4 in the paper.
     */
    private long getInterference(Task task_j, int R_prev, Core core, Task task_i) {
        // int C_i = (int) task_i.bodyTime;
        long T_j = task_j.period;
        int C_j = (int) task_j.bodyTime;
        int C_i = (int) task_i.bodyTime;
        long T_i = task_i.period;
        double w_i = task_i.weight;
        long lastRequestTime = Math.floorDiv(R_prev, T_j) * T_j; // (R_prev / T_j) * T_j
        // int remainWorkload = getRemainWorkload(lastRequestTime, C_i, task_i.id, core);
        int remainWorkload = C_i;
        if (lastRequestTime > 0){
            remainWorkload = getRemainWorkload_v2(lastRequestTime, task_i, core);
        }
        long S_j = 0;
        if(remainWorkload != 0){
            S_j = getS_j(task_j, C_j, remainWorkload, w_i, T_i, core.minWeight);
        }

        return (Math.floorDiv(R_prev, T_j) * C_j) + S_j;
    }

    /*
     * Lemma3 in the paper.
     * \alpha =  L \cdot \frac{w_j}{w_i + w_{min}}, \quad
     * \beta =  \Delta_{i}^{t_{s(j,q)}} \cdot \frac{w_j}{w_i},  \quad
     * \gamma = L \cdot \frac{w_j}{w_i + w_j} 
     */
    private long getS_j(Task task_j, int C_j, int remainWorkload, double w_i, long T_i, double minWeight) {
        double w_j = task_j.weight;
        long T_j = task_j.period;
        int alpha = (int) (this.targetLatency * w_j / (w_i + minWeight));
        int beta = (int) (remainWorkload * w_j / w_i);
        int gamma = (int) (this.targetLatency * w_j / (w_i + w_j));
        if (T_j > T_i || T_j == T_i) {
            return (int) Math.min(beta + gamma, C_j);
        }else{
            return (int) Math.min(alpha + beta + gamma, C_j);
        }
    }


    /*
     * Lemma6 in the paper.
     */
    private int getRemainWorkload(long lastRequestTime, double C_i, int task_i_id, Core core) {
        int maxInterferenceUntilLastRequestTime = 0;
        for (Task task_k : core.tasks) {
            if (task_i_id != task_k.id) {
                long T_k = task_k.period;
                int C_k = (int) task_k.bodyTime;
                int interference_k = (int) ((Math.floorDiv(lastRequestTime, T_k)) * C_k);
                interference_k += Math.min(C_k, lastRequestTime % T_k);
                maxInterferenceUntilLastRequestTime += interference_k;
            }
        }
        return (int) (C_i - (lastRequestTime - maxInterferenceUntilLastRequestTime));
    }

    private int getRemainWorkload_v2(long lastRequestTime, Task task_i, Core core) {
        int max_min_task_i_processed = 0;
        int C_i = (int) task_i.bodyTime;
        double w_i = task_i.weight;
        for (Task task_k : core.tasks) {
            if (task_i.id != task_k.id) {
                long T_k = task_k.period;
                double w_k = task_k.weight;
                int C_k = (int) task_k.bodyTime;
                if (T_k > lastRequestTime){
                    continue;
                }else{
                    // int min_task_i_processed = (int) (Math.floorDiv(lastRequestTime, T_k) * C_k * w_i / w_k) - (int) (this.targetLatency * w_i / (w_i + w_k));
                    int min_task_i_processed = (int) (Math.floorDiv(lastRequestTime, T_k) * C_k * w_i / w_k);
                    if (min_task_i_processed > max_min_task_i_processed){
                        max_min_task_i_processed = min_task_i_processed;
                    }
                }
            }
        }
        if(max_min_task_i_processed > C_i){
            return 0;
        }else{
            return (int) C_i - max_min_task_i_processed;
        }

    }
}
