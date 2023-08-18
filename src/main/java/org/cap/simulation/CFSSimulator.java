package org.cap.simulation;

import org.cap.model.*;
import org.cap.utility.LoggerUtility;
import org.cap.utility.MathUtility;

import java.util.*;
import java.util.logging.Logger;

public class CFSSimulator {
    private static final Logger logger = LoggerUtility.getLogger();

    // TODO refactor to simulation state later
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

        performSimulation(cores, queues, time);

        LoggerUtility.addConsoleLogger();
        return new SimulationResult(schedulability);
    }

    private void performSimulation(List<Core> cores, List<Queue<Task>> queues, int time) {
        int hyperperiod = MathUtility.getLCM(cores);

        while (time < hyperperiod) {
            for (int i = 0; i < cores.size(); i++) {
                // TODO add periodic jobs
                Queue<Task> queue = queues.get(i);
                Task task = selectTask(queue); // selectTask 함수 안에 minimum virtual runtime 로직 존재
                if (task == null)
                    continue;

                executeTask(task, queue, time);
            }
            time++;
        }
    }

    private void executeTask(Task task, Queue<Task> queueInCore, int time) {
        logger.info("Task " + task.id + " executed for 1 in stage: " + task.stage);

        // Decrease runtime
        remainingRuntime--;
        if (remainingRuntime <= 0)
            isRunning = false;

        // Update virtual runtime
        task.virtualRuntime += 1024/ task.weight;

        // Decrease execution time for each stage
        switch (task.stage) {
            case READ:
                task.readTime--;
                if (task.readTime <= 0) {
                    task.stage = Stage.BODY;
                    task.bodyReleaseTime = time + 1;
                }
                else {
                    if (isRunning)
                        isReadBlocked = true;
                }
                break;
            case BODY:
                task.bodyTime--;
                if (task.bodyTime <= 0) {
                    if (task.writeTime > 0) {
                        task.stage = Stage.WRITE;
                        task.writeReleaseTime = time + 1;
                    }
                    else {
                        task.stage = Stage.COMPLETED;
                        isRunning = false;
                    }
                }
                break;
            case WRITE:
                task.writeTime--;
                if (task.writeTime <= 0) {
                    task.stage = Stage.COMPLETED;
                    isRunning = false;
                }
                else
                    isWriteBlocked = true;
                break;
        }

        // Add task back to queue if task is not finished but runtime is over
        if (task.stage != Stage.COMPLETED) {
            if (isRunning)
                currentTask = task;
            else
                queueInCore.add(task);
        } else {
            logger.info("Task " + task.id + " completed at time " + (time + 1) + " with RT " + (time - task.readReleaseTime + 1));
            task.WCRT_by_CFS_simulator = time + 1 - task.readReleaseTime;
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

    private Task selectTask(Queue<Task> queueInCore) {
        // Case 1: running task already exists
        if (isRunning) {
            return currentTask;
        }
        // Case 2: select a new task if a new task is released
        // TODO consider blocking policy
        else {
            // TODO check if diverging is needed
            Task task = queueInCore.poll();
            if (task == null)
                return null;

            double totalWeight = queueInCore.stream().mapToDouble(t -> t.weight).sum();
            remainingRuntime = (int) Math.max(targetedLatency * task.weight / totalWeight, minimumGranularity);
            isRunning = true;
            return task;
        }
    }
}
