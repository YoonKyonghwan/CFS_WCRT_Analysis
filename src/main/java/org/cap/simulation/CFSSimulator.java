package org.cap.simulation;

import org.cap.model.*;
import org.cap.utility.LoggerUtility;
import org.cap.utility.MathUtility;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class CFSSimulator {
    private static final Logger logger = LoggerUtility.getLogger();

    public SimulationResult simulateCFS(List<Core> cores) {
        LoggerUtility.initializeLogger();
        logger.info("Starting CFS simulation");

        boolean schedulability = true;
        int time = 0;
        // TODO receive as parameters
        int targetedLatency = 20;
        int minimumGranularity = 4;
        List<Queue<Task>> queues = initializeQueues(cores);

        performSimulation(cores, queues, targetedLatency, minimumGranularity, time);

        LoggerUtility.addConsoleLogger();
        return new SimulationResult(schedulability);
    }

    private void performSimulation(List<Core> cores, List<Queue<Task>> queues, int targetedLatency, int minimumGranularity, int time) {
        // TODO CFS simulation
        // TODO set vruntime
        int hyperperiod = MathUtility.getLCM(cores);
        while (time < hyperperiod) {
            logger.info(String.format("\n>>> CURRENT TIME: %d <<<\n", time));
            List<List<Task>> runningTasks = initializeRunningTasks(queues, time);

            // TODO if empty, increase time 

            for (int i = 0; i < cores.size(); i++) {
                List<Task> runningTasksInCore = runningTasks.get(i);
                double totalWeight = runningTasksInCore.stream().mapToDouble(t -> t.weight).sum();
                for (Task task : runningTasksInCore) {
                    // Iterate in order of minimum vruntime
                    double runtime = targetedLatency * (task.weight / totalWeight);
                    if (runtime < minimumGranularity)
                        runtime = minimumGranularity;
                    // Execute task for runtime by subtracting runtime from body time
                    task.bodyTime -= runtime;
                    if (MathUtility.withinTolerance(task.bodyTime, 0)) {
                        // If task finishes, update WCRT
                        // Update runtime
                    }
                    // Update virtualRuntime
                    task.virtualRuntime += runtime * (1024/task.weight);
                    // Increase time
                    time += runtime;
                    // Reschedule if task is released or task finishes
                }
            }
        }
    }

    private List<Queue<Task>> initializeQueues(List<Core> cores) {
        List<Queue<Task>> queues = new ArrayList<>();
        for (Core core : cores) {
            Queue<Task> queueInCore = new PriorityQueue<>(Comparator.comparingDouble(task -> task.virtualRuntime));
            for (Task task : core.tasks) {
                task.weight = NiceToWeight.getWeight(task.nice);
                task.originalBodyTime = task.bodyTime;
                queueInCore.add(task.copy());
            }
            queues.add(queueInCore);
        }
        return queues;
    }

    private List<List<Task>> initializeRunningTasks(List<Queue<Task>> queues, int time) {
        List<List<Task>> runningTasks = new ArrayList<>();

        for (Queue<Task> queueInCore : queues) {
            Iterator<Task> iterator = queueInCore.iterator();
            List<Task> runningTasksInCore = new ArrayList<>();
            while (iterator.hasNext()) {
                Task task = iterator.next();
                if (task.startTime > time)
                    continue;
                runningTasksInCore.add(task);
                iterator.remove();
            }

            runningTasks.add(runningTasksInCore);
            logger.info("Running tasks in core " + runningTasks.size() + ": " + runningTasksInCore.stream().map(task -> task.id).collect(Collectors.toList()));
        }

        return runningTasks;
    }

}
