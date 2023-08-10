package org.cap.simulation;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    private boolean is_min_timeslice;
    private static final int min_timeslice = 2;
    private static final int max_num_threads = 8;

    public boolean analyze(List<Core> cores, boolean verbose, boolean is_min_timeslice) {
        this.cores = cores;
        this.is_min_timeslice = is_min_timeslice;

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
                        if (task.Eir == -1) {
                            System.out.println("E_ir of task " + task.id + " is larger than its period");
                        }
                        if(task.Eiw == -1){
                            System.out.println("E_iw of task " + task.id + " is larger than its period");
                        }
                        if(task.Eib == -1){
                            System.out.println("E_ib of task " + task.id + " is larger than its period");
                        }
                    }
                    return false;
                }
            }
        }

        for (Core core : this.cores) {
            for (Task task : core.tasks) {
                task.WCRT_by_proposed = computeRr(task) + computeRw(task) + computeRb(task);
                if (task.WCRT_by_proposed > task.period){
                    if (verbose) {
                        System.out.println("The WCRT of task " + task.id + " is larger than its period" +
                                " (WCRT: " + task.WCRT_by_proposed + " period: " + task.period + ")");
                    }
                    return false;
                }
            }
        }
        
        return true;
    }


    public boolean analyze_parallel(List<Core> cores, boolean verbose, boolean is_min_timeslice) {
        this.cores = cores;
        this.is_min_timeslice = is_min_timeslice;

        // convert nice_value to weight
        for (Core core : this.cores) {
            for (Task task : core.tasks) {
                task.priorityWeight = priorityToWeight.get(task.nice + 20);
            }
        }
        
        // compute E for each task in parallel
        ExecutorService threads_for_E = Executors.newFixedThreadPool(max_num_threads);
        for (Core core : this.cores) {
            for (Task task : core.tasks) {
                Runnable task_computeE = new Runnable() {
                    @Override
                    public void run() {
                        task.Eir = computeEr(core, task.id);
                        task.Eiw = computeEw(core, task.id);
                        task.Eib = computeEb(core, task.id);
                        if (verbose) {
                            if (task.Eir == -1) {
                                System.out.println("E_ir of task " + task.id + " is larger than its period");
                            }
                            if(task.Eiw == -1){
                                System.out.println("E_iw of task " + task.id + " is larger than its period");
                            }
                            if(task.Eib == -1){
                                System.out.println("E_ib of task " + task.id + " is larger than its period");
                            }
                        }
                    }
                };
                threads_for_E.execute(task_computeE);
            }
        }
        threads_for_E.shutdown();

        // check schedulability in terms of E
        for (Core core : this.cores) {
            for (Task task : core.tasks) {
                if (task.Eir == -1 || task.Eiw == -1 || task.Eib == -1) return false;
            }
        }

        // compute R for each task in parallel
        ExecutorService threads_for_R = Executors.newFixedThreadPool(max_num_threads);
        for (Core core : this.cores) {
            for (Task task : core.tasks) {
                // compute Rir, Riw, Rib with executors parallely
                Runnable task_computeR = new Runnable() {
                    @Override
                    public void run() {
                        task.WCRT_by_proposed = computeRr(task) + computeRw(task) + computeRb(task);
                        if (verbose) {
                            if (task.WCRT_by_proposed > task.period) {
                                System.out.println("The WCRT of task " + task.id + " is larger than its period" +
                                        " (WCRT: " + task.WCRT_by_proposed + " period: " + task.period + ")");
                            }
                        }
                    }
                };
                threads_for_R.execute(task_computeR);
            }
        }
        threads_for_R.shutdown();

        // check schedulability in terms of E
        for (Core core : this.cores) {
            for (Task task : core.tasks) {
                if (task.WCRT_by_proposed > task.period) return false;
            }
        }

        return true;
    }


    /* 
    Equation1 in the paper.
    */
    private int computeInterference(Task i, Task j, int t) {
        double Tj = j.period;
        double Cj = j.readTime + j.bodyTime + j.writeTime;
        double wj = j.priorityWeight;
        double wi = i.priorityWeight;

        double I = Math.floor(t / Tj) * Cj;
        double computed_interfernce = (wj / (wi + wj)) * (t % Tj);
        double max_interference = Cj;

        I += postprocessInterference(computed_interfernce, max_interference);

        return (int) Math.ceil(I);
    }


    /* 
    Equation2 in the paper.
    */
    private int computeIr(Task task_i, Task task_j, int t){
        double Cjr = task_j.readTime;
        double Cjb = task_j.bodyTime;
        double Cjw = task_j.writeTime;
        double wj = task_j.priorityWeight;
        double wi = task_i.priorityWeight;

        double I = 0;
        if (Cjw == 0) {
            I = computeInterference(task_i, task_j, t);
        } else {
            double computed_interfernce = (wj / (wi + wj)) * t;
            double max_interference = Cjr + Cjb;
            I = postprocessInterference(computed_interfernce, max_interference);
        }
        return (int) Math.ceil(I);
    }


    /* 
    Equation4 in the paper.
    */
    private int computeIw(Task task_i, Task task_j, int t){
        double Cjr = task_j.readTime;
        double Cjb = task_j.bodyTime;
        double Cjw = task_j.writeTime;
        double wj = task_j.priorityWeight;
        double wi = task_i.priorityWeight;

        double I = 0;
        if (Cjr == 0 && Cjw == 0) {
            I = computeInterference(task_i, task_j, t);
        } else {
            double computed_interfernce = (wj / (wi + wj)) * t;
            double max_interference = Cjb;
            I = postprocessInterference(computed_interfernce, max_interference);
        }
        return (int) Math.ceil(I);
    }


    /* 
    Equation3 in the paper.
    */
    private int computeEr(Core core, int task_id){
        Task task_i = getTask_i(core, task_id);

        int Eir = (int) task_i.readTime; // initial Eir
        int Eir_prev = 0;
        // compute Eir iteratively until Eir_k == Eir_k+1        
        while (Eir_prev != Eir) {
            Eir_prev = Eir;
            Eir = (int) task_i.readTime;
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
    private int computeEw(Core core, int task_id){
        Task task_i = getTask_i(core, task_id);

        int Eiw = (int) task_i.writeTime; // initial Eiw
        int Eiw_prev = 0;
        // compute Eiw iteratively until Eiw_k == Eiw_k+1
        while (Eiw_prev != Eiw) {
            Eiw_prev = Eiw;
            Eiw = (int) task_i.writeTime;
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
    private int computeEb(Core core, int task_id){
        Task task_i = getTask_i(core, task_id);

        int Eib = (int) task_i.bodyTime; // initial Eib
        int Eib_prev = 0;
        // compute Eib iteratively until Eib_k == Eib_k+1
        while (Eib_prev != Eib) {
            Eib_prev = Eib;
            Eib = (int) task_i.bodyTime;
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
    private int computeRr(Task task_i){
        int Rir = task_i.Eir;
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
    private int computeRb(Task task_i){
        return task_i.Eib;
    }


    /* 
    Equation8 in the paper
    */
    private int computeRw(Task task_i){
        int Riw = task_i.Eiw;
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


    private double postprocessInterference(double computed_interfernce, double max_interference) {
        double interference = 0;
        if (computed_interfernce > max_interference) {
            interference = max_interference;
        }else{
            if (is_min_timeslice){
                if (computed_interfernce > min_timeslice){
                    interference = computed_interfernce;
                }else{
                    if(max_interference > min_timeslice){
                        interference = min_timeslice;
                    }else{
                        interference = max_interference;
                    }
                }
            }else{
                interference = computed_interfernce;
            }
        }
        return interference;
    }
}
