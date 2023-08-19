package org.cap.simulation;

import org.cap.model.*;
import org.cap.utility.LoggerUtility;
import org.cap.utility.MathUtility;

import java.util.*;
import java.util.logging.Logger;

public class CFSSimulator {
    private static final Logger logger = LoggerUtility.getLogger();

    public SimulationResult simulateCFS(List<Core> cores) {
        LoggerUtility.initializeLogger();
        logger.info("Starting CFS simulation");

        boolean schedulability = true;
        CFSSimulationState simulationState = new CFSSimulationState(20, 4);
        int time = 0;
        // TODO consider using a red-black tree or other data structures
        List<Queue<Task>> queues = initializeQueues(cores);

        performSimulation(cores, queues, simulationState, time);

        LoggerUtility.addConsoleLogger();
        // TODO modify schedulability
        return new SimulationResult(schedulability);
    }

    private void performSimulation(List<Core> cores, List<Queue<Task>> queues, CFSSimulationState simulationState, int time) {
        int hyperperiod = MathUtility.getLCM(cores);

        while (time < hyperperiod) {
            addJobs(cores, queues, time);

            for (int i = 0; i < cores.size(); i++) {
                Queue<Task> queue = queues.get(i);
                Task task = selectTask(queue, simulationState);
                if (task == null)
                    continue;

                executeTask(task, queue, simulationState, time);
            }

            time++;
        }
    }

    private void executeTask(Task task, Queue<Task> queueInCore, CFSSimulationState simulationState, int time) {
        logger.info("Task " + task.id + " executed for 1 in stage: " + task.stage);

        // Decrease runtime
        simulationState.remainingRuntime--;
        if (simulationState.remainingRuntime <= 0)
            simulationState.isRunning = false;

        // Update virtual runtime
        task.virtualRuntime += 1024/ task.weight;

        // Decrease execution time for each stage
        switch (task.stage) {
            case READ:
                task.readTime--;
                if (task.readTime <= 0) {
                    task.stage = Stage.BODY;
                    task.bodyReleaseTime = time + 1;
                    simulationState.blockingPolicy = BlockingPolicy.NONE;
                }
                else {
                    if (simulationState.isRunning)
                        simulationState.blockingPolicy = BlockingPolicy.READ;
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
                        simulationState.isRunning = false;
                    }
                }
                break;
            case WRITE:
                task.writeTime--;
                if (task.writeTime <= 0) {
                    task.stage = Stage.COMPLETED;
                    simulationState.isRunning = false;
                    simulationState.blockingPolicy = BlockingPolicy.NONE;
                }
                else
                    simulationState.blockingPolicy = BlockingPolicy.WRITE;
                break;
        }

        // Add task back to queue if task is not finished but runtime is over
        if (task.stage != Stage.COMPLETED) {
            if (simulationState.isRunning)
                simulationState.currentTask = task;
            else {
                queueInCore.add(task);
                if (simulationState.blockingPolicy == BlockingPolicy.READ)
                    simulationState.blockingPolicy = BlockingPolicy.NONE;
            }
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
                task.originalReadTime = task.readTime;
                task.originalBodyTime = task.bodyTime;
                task.originalWriteTime = task.writeTime;
                task.readReleaseTime = task.startTime;
                skipReadStageIfNoReadTime(task);
                if (task.startTime == 0)
                    queueInCore.add(task.copy());
            }
            queues.add(queueInCore);
        }
        return queues;
    }

    private void addJobs(List<Core> cores, List<Queue<Task>> queues, int time) {
        for (Core core : cores) {
            for (Task task : core.tasks) {
                if (initialJobs(time, task) || periodicJobs(time, task)) {
                    logger.info("Task " + task.id + " released with read time " + task.readTime + ", body Time " + task.bodyTime + ", write time " + task.writeTime);
                    task.readReleaseTime = time;
                    skipReadStageIfNoReadTime(task);
                    queues.get(core.id-1).add(task.copy());

                }
            }
        }
    }

    private boolean initialJobs(int time, Task task) {
        return task.startTime > 0 && time == task.startTime;
    }

    private boolean periodicJobs(int time, Task task) {
        return time > task.startTime && task.period > 0 && (time - task.startTime) % task.period == 0;
    }

    private void skipReadStageIfNoReadTime(Task task) {
        if (task.stage == Stage.READ && task.readTime <= 0) {
            task.stage = Stage.BODY;
            task.bodyReleaseTime = task.readReleaseTime;
        }
    }

    private Task selectTask(Queue<Task> queueInCore, CFSSimulationState simulationState) {
        // Case 1: running task already exists
        if (simulationState.isRunning) {
            return simulationState.currentTask;
        }
        // Case 2: select a new task if a new task is released
        else {
            if (queueInCore.isEmpty())
                return null;

            Task task = null;
            List<Task> minRuntimeTasks = new ArrayList<>();
            double minRuntime;
            switch (simulationState.blockingPolicy) {
                case NONE:
                    minRuntime = queueInCore.peek().virtualRuntime;
                    while (!queueInCore.isEmpty() && queueInCore.peek().virtualRuntime == minRuntime)
                        minRuntimeTasks.add(queueInCore.poll());
                    break;
                case READ:
                    List<Task> readTasks = new ArrayList<>();
                    queueInCore.removeIf(t -> {
                        if (t.stage == Stage.READ) {
                            readTasks.add(t);
                            return true;
                        }
                        return false;
                    });

                    minRuntime = queueInCore.peek().virtualRuntime;
                    while (!queueInCore.isEmpty() && queueInCore.peek().virtualRuntime == minRuntime)
                        minRuntimeTasks.add(queueInCore.poll());

                    queueInCore.addAll(readTasks);
                    break;
                case WRITE:
                    List<Task> writeTasks = new ArrayList<>();
                    queueInCore.removeIf(t -> {
                        if (t.stage == Stage.WRITE) {
                            writeTasks.add(t);
                            return true;
                        }
                        return false;
                    });

                    minRuntime = queueInCore.peek().virtualRuntime;
                    while (!queueInCore.isEmpty() && queueInCore.peek().virtualRuntime == minRuntime)
                        minRuntimeTasks.add(queueInCore.poll());

                    queueInCore.addAll(writeTasks);
                    break;
            }

            // May need to revert to WCRTs since task has to be copied
            if (minRuntimeTasks.size() > 1)
                // TODO diverge
                diverge();
            else
                task = minRuntimeTasks.get(0);

            double totalWeight = queueInCore.stream().mapToDouble(t -> t.weight).sum();
            simulationState.remainingRuntime = (int) Math.max(simulationState.targetedLatency * task.weight / totalWeight, simulationState.minimumGranularity);
            simulationState.isRunning = true;
            return task;
        }
    }

    // TODO receive simulation state
    private void diverge() {
    }
}
