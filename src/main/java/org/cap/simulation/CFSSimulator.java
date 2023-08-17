package org.cap.simulation;

import org.cap.model.*;
import org.cap.utility.LoggerUtility;
import org.cap.utility.MathUtility;

import java.util.*;
import java.util.logging.Logger;

public class CFSSimulator {
    private static final Logger logger = LoggerUtility.getLogger();

    // TODO refactor to simulation state later
    // TODO remainingRuntime
    boolean isReadBlocked = false;
    boolean isWriteBlocked = false;
    Task currentTask = null;
    boolean isRunning = false;
    int remainingRuntime = 0;
    // TODO receive as parameters
    int targetedLatency = 20;
    int minimumGranularity = 4;

    public SimulationResult simulateCFS(List<Core> cores) {
        LoggerUtility.initializeLogger();
        logger.info("Starting CFS simulation");

        boolean schedulability = true;
        int time = 0;
        // TODO consider using red-black trees
        List<Queue<Task>> queues = initializeQueues(cores);

        performSimulation(cores, queues, targetedLatency, minimumGranularity, time);

        LoggerUtility.addConsoleLogger();
        return new SimulationResult(schedulability);
    }

    private void performSimulation(List<Core> cores, List<Queue<Task>> queues, int targetedLatency, int minimumGranularity, int time) {
        int hyperperiod = MathUtility.getLCM(cores);

        // TODO apply read, body, write

        while (time < hyperperiod) {
            for (int i = 0; i < cores.size(); i++) {
                Queue<Task> queue = queues.get(i);
                Task task = selectTask(queue); // selectTask 함수 안에 minimum virtual runtime 로직 존재
                if (task == null)
                    continue;

                task.bodyTime--;
                if (task.bodyTime <= 0) {
                    task.WCRT_by_CFS_simulator = time + 1 - task.startTime;
                    isRunning = false;
                    remainingRuntime = 0;
                }
                else {
                    remainingRuntime--;
                    if (remainingRuntime <= 0)
                        isRunning = false;
                    // TODO add task back to queue and reschedule after modifying virtual runtime
                }
                // The vruntime variable is updated after each run of the process.
                task.virtualRuntime += 1024/task.weight;
            }
            time++;
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

    private Task selectTask(Queue<Task> queue) {
        // Case 1: running task already exists
        // Use a flag
        if (isRunning) {
            return currentTask;
        }
        // Case 2: select a new task if a new task is released
        // Choose task with minimum virtual runtime
        // runtime with following formula: runtime = max(targetedLatency * (task.weight / totalWeight), minimumGranularity)
        else {
            // TODO check if diverging is needed
            Task task = queue.poll();
            if (task == null)
                return null;

            double totalWeight = queue.stream().mapToDouble(t -> t.weight).sum();
            remainingRuntime = (int) Math.max(targetedLatency * task.weight / totalWeight, minimumGranularity);
            isRunning = true;
            return task;
        }
    }
}
