package org.cap.simulation;

import java.util.Arrays;
import java.util.List;
import org.cap.model.Core;
import org.cap.model.Task;

public class Analyzer {
    private List<Core> cores;

    private static final List<Integer> priorityToWeight = Arrays.asList(
        88761, 71755, 56483, 46273, 36291,
        29154, 23254, 18705, 14949, 11916,
        9548, 7620, 6100, 4904, 3906,
        3121, 2501, 1991, 1586, 1277,
        1024, 820, 655, 526, 423,
        335, 272, 215, 172, 137,
        110, 87, 70, 56, 45,
        36, 29, 23, 18, 15
    );

    private static final double min_granularity = 1;

    public boolean analyze(List<Core> cores, boolean verbose) {
        this.cores = cores;

        // convert nice_value to weight
        for (Core core : this.cores) {
            for (Task task : core.tasks) {
                task.priorityWeight = priorityToWeight.get(task.nice + 20);
            }
        }

        // compute Eir, Eiw, Eib for each task
        for (Core core : this.cores) {
            for (Task task : core.tasks) {
                task.Eir = computeEr(core, task.id);
                task.Eiw = computeEw(core, task.id);
                task.Eib = computeEb(core, task.id);

                if (task.Eir == -1 || task.Eiw == -1 || task.Eib == -1) {
                    if (verbose) {
                        System.out.println("Task " + task.id + " in core " + core.id + " is not schedulable");
                    }
                    return false;
                }
            }
        }

        for (Core core : this.cores) {
            for (Task task : core.tasks) {
                double Rir = computeRr(task);
                double Riw = computeRw(task);
                double Rib = computeRb(task);

                if (Rir+Riw+Rib > task.period){
                    if (verbose) {
                        System.out.println("Task " + task.id + " in core " + core.id + " is not schedulable");
                    }
                    return false;
                }
            }
        }
        
        return true;
    }


    /* 
    Equation1 in the paper.
    */
    private double computeInterference(Task i, Task j, double t) {
        double Tj = j.period;
        double Cj = j.readTime + j.bodyTime + j.writeTime;
        double wj = j.priorityWeight;
        double wi = i.priorityWeight;

        double I = Math.floor(t / Tj) * Cj;
        double candidate1 = ceilByGranularity((wj / (wi + wj)) * (t % Tj));
        double candidate2 = Cj;
        I += Math.min(candidate1, candidate2);

        return I;
    }


    /* 
    Equation2 in the paper.
    */
    private double computeIr(Task task_i, Task task_j, double t){
        double Cjr = task_j.readTime;
        double Cjb = task_j.bodyTime;
        double Cjw = task_j.writeTime;
        double wj = task_j.priorityWeight;
        double wi = task_i.priorityWeight;

        double I = 0;
        if (Cjw == 0) {
            I = computeInterference(task_i, task_j, t);
        } else {
            double candidate1 = ceilByGranularity((wj / (wi + wj)) * t);
            double candidate2 = Cjr + Cjb;
            I = Math.min(candidate1, candidate2);
        }
        return I;
    }


    /* 
    Equation4 in the paper.
    */
    private double computeIw(Task task_i, Task task_j, double t){
        double Cjr = task_j.readTime;
        double Cjb = task_j.bodyTime;
        double Cjw = task_j.writeTime;
        double wj = task_j.priorityWeight;
        double wi = task_i.priorityWeight;

        double I = 0;
        if (Cjr == 0 && Cjw == 0) {
            I = computeInterference(task_i, task_j, t);
        } else {
            double candidate1 = ceilByGranularity((wj / (wi + wj)) * t);
            double candidate2 = Cjb;
            I = Math.min(candidate1, candidate2);
        }
        return I;
    }


    /* 
    Equation3 in the paper.
    */
    private double computeEr(Core core, int task_id){
        Task task_i = getTask_i(core, task_id);

        double Eir = task_i.readTime; // initial Eir
        double Eir_prev = 0;
        // compute Eir iteratively until Eir_k == Eir_k+1        
        while (Eir_prev != Eir) {
            Eir_prev = Eir;
            Eir = task_i.readTime;
            for (Task task_j : core.tasks) {
                if (task_i.id != task_j.id) {
                    Eir += computeIr(task_i, task_j, Eir_prev);
                }
            }

            if (Eir > task_i.period) {
                return -1;
            }
        }

        return Eir;
    }


    /* 
    Equation5 in the paper.
    */
    private double computeEw(Core core, int task_id){
        Task task_i = getTask_i(core, task_id);

        double Eiw = task_i.writeTime; // initial Eiw
        double Eiw_prev = 0;
        // compute Eiw iteratively until Eiw_k == Eiw_k+1
        while (Eiw_prev != Eiw) {
            Eiw_prev = Eiw;
            Eiw = task_i.writeTime;
            for (Task task_j : core.tasks) {
                if (task_i.id != task_j.id) {
                    Eiw += computeIw(task_i, task_j, Eiw_prev);
                }
            }

            if (Eiw > task_i.period) {
                return -1;
            }
        }

        return Eiw;
    }


    /* 
    Equation7 in the paper.
    */
    private double computeEb(Core core, int task_id){
        Task task_i = getTask_i(core, task_id);

        double Eib = task_i.bodyTime; // initial Eib
        double Eib_prev = 0;
        // compute Eib iteratively until Eib_k == Eib_k+1
        while (Eib_prev != Eib) {
            Eib_prev = Eib;
            Eib = task_i.bodyTime;
            for (Task task_j : core.tasks) {
                if (task_i.id != task_j.id) {
                    Eib += computeInterference(task_i, task_j, Eib_prev);
                }
            }

            if (Eib > task_i.period) {
                return -1;
            }
        }
        return Eib;
    }    


    /* 
    Equation6 in the paper
    */
    private double computeRr(Task task_i){
        double Rir = task_i.Eir;
        for (Core core : this.cores){
            for (Task task_j : core.tasks){
                if (task_i.id != task_j.id){
                    Rir += task_j.Eiw;
                }
            }
        }
        return Rir;
    }


    /* 
    Equation7 in the paper
    */
    private double computeRb(Task task_i){
        return task_i.Eib;
    }


    /* 
    Equation8 in the paper
    */
    private double computeRw(Task task_i){
        double Riw = task_i.Eiw;
        for (Core core : this.cores){
            for (Task task_j : core.tasks){
                if (task_i.id != task_j.id){
                    Riw += Math.max(task_j.Eir, task_j.Eiw);
                }
            }
        }
        return Riw;
    }


    private Task getTask_i(Core core, int task_id) {
        int task_index_in_core = -1;
        for (int i = 0; i < core.tasks.size(); i++) {
            if (core.tasks.get(i).id == task_id) {
                task_index_in_core = i;
                break;
            }
        }

        if (task_index_in_core == -1) {
            System.out.println(task_id + " is not found in core " + core.id);
            System.exit(1);
        }

        Task task_i = core.tasks.get(task_index_in_core);
        assert task_i.id == task_id;
        return task_i;
    }

    private double ceilByGranularity(double x) {
        return Math.ceil(x / min_granularity) * min_granularity;
    }
}
