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


    // public boolean analyze_parallel(List<Core> cores, boolean verbose, boolean is_min_timeslice) {
    //     this.cores = cores;

    //     // convert nice_value to weight
    //     for (Core core : this.cores) {
    //         for (Task task : core.tasks) {
    //             task.weight = NiceToWeight.getWeight(task.nice);
    //         }
    //     }
        
    //     // compute E for each task in parallel
    //     ExecutorService threads_for_E = Executors.newFixedThreadPool(max_num_threads);
    //     for (Core core : this.cores) {
    //         for (Task task : core.tasks) {
    //             Runnable task_computeE = new Runnable() {
    //                 @Override
    //                 public void run() {
    //                     computeE(core, task, verbose);
    //                 }
    //             };
    //             threads_for_E.execute(task_computeE);
    //         }
    //     }
    //     threads_for_E.shutdown();
    //     return checkSchedulabilityByE();
    // }


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
                    int interference = getInterference(task_j, R_prev, core, task_i);
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
    private int getInterference(Task task_j, int R_prev, Core core, Task task_i) {
        int C_i = (int) task_i.bodyTime;
        int T_j = task_j.period;
        int C_j = (int) task_j.bodyTime;
        int T_i = task_i.period;
        double w_i = task_i.weight;
        int lastRequestTime = R_prev - (R_prev % T_j); // (R_prev / T_j) * T_j
        int remainWorkload = getRemainWorkload(lastRequestTime, C_i, task_i.id, core);
        int S_j = getS_j(task_j, C_j, remainWorkload, w_i, T_i, core.minWeight);

        return (((int)(R_prev / T_j)) * C_j) + S_j;
    }

    /*
     * Lemma3 in the paper.
     * \alpha =  L \cdot \frac{w_j}{w_i + w_{min}}, \quad
     * \beta =  \Delta_{i}^{t_{s(j,q)}} \cdot \frac{w_j}{w_i},  \quad
     * \gamma = L \cdot \frac{w_j}{w_i + w_j} 
     */
    private int getS_j(Task task_j, int C_j, int remainWorkload, double w_i, int T_i, double minWeight) {
        double w_j = task_j.weight;
        int T_j = task_j.period;
        int alpha = (int) (this.targetLatency * (w_j / w_i + minWeight));
        int beta = (int) (remainWorkload * (w_j / w_i));
        int gamma = (int) (this.targetLatency * (w_j / (w_i + w_j)));
        if (T_j > T_i || T_j == T_i) {
            return (int) Math.min(alpha + beta + gamma, C_j);
        }else{
            return (int) Math.min(beta + gamma, C_j);
        }
    }

    /*
     * Lemma6 in the paper.
     */
    private int getRemainWorkload(int lastRequestTime, double C_i, int task_i_id, Core core) {
        int maxInterferenceUntilLastRequestTime = 0;
        for (Task task_k : core.tasks) {
            if (task_i_id != task_k.id) {
                int T_k = task_k.period;
                int C_k = (int) task_k.bodyTime;
                int interference_k = (((int)(lastRequestTime / T_k)) * C_k);
                interference_k += Math.min(C_k, lastRequestTime % T_k);
                maxInterferenceUntilLastRequestTime += interference_k;
            }
        }
        return (int) (C_i - (lastRequestTime - maxInterferenceUntilLastRequestTime));
    }
}
