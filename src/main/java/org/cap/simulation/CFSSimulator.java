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
        logger.info("\n------------------------------");
        logger.info("*** CFS Simulation Started ***");
        logger.info("------------------------------");

        List<List<Double>> WCRTs = initializeWCRTs(cores);
        List<Queue<Task>> queues = initializeQueues(cores);
        CFSSimulationState simulationState = new CFSSimulationState(20, 4, cores.size());
        int time = 0;
        int hyperperiod = MathUtility.getLCM(cores);

        performSimulation(cores, queues, WCRTs, simulationState, time, hyperperiod);

        return checkSchedulability(cores, queues, WCRTs);
    }

    private void performSimulation(List<Core> cores, List<Queue<Task>> queues, List<List<Double>> WCRTs, CFSSimulationState simulationState, int time, int hyperperiod) {
        outerLoop:
        while (time < 2 * hyperperiod) {
            logger.info("\nTime " + time + ":");
            addJobs(cores, queues, time);

            for (int i = 0; i < cores.size(); i++) {
                Queue<Task> queue = queues.get(i);
                List<Double> WCRT = WCRTs.get(i);
                CoreState coreState = simulationState.coreStates.get(i);

                Task task = null;
                if (coreState.isRunning)
                    task = coreState.currentTask;
                else {
                    List<Task> minRuntimeTasks = getMinRuntimeTasks(queue, simulationState);
                    if (minRuntimeTasks.size() > 1) {
                        pathDiverges(i, minRuntimeTasks, cores, queues, WCRTs, simulationState, time, hyperperiod);
                        break outerLoop;
                    }
                    else if (minRuntimeTasks.size() == 1)
                        task = minRuntimeTasks.get(0);
                }
                if (task == null)
                    continue; 
                setRuntime(i, task, queue, simulationState);
                executeTask(task, queue, WCRT, simulationState, coreState, time);
            }

            time++;
        }
    }

    private void executeTask(Task task, Queue<Task> queueInCore, List<Double> WCRTInCore, CFSSimulationState simulationState, CoreState coreState, int time) {
        logger.info("- Task " + task.id + " executed in stage: " + task.stage);

        // Decrease runtime
        coreState.remainingRuntime--;
        if (coreState.remainingRuntime <= 0)
            coreState.isRunning = false;

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
                    // Need to set after the second ends to avoid blocking canceled earlier
                }
                else {
                    if (coreState.isRunning)
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
                        coreState.isRunning = false;
                    }
                }
                break;
            case WRITE:
                task.writeTime--;
                if (task.writeTime <= 0) {
                    task.stage = Stage.COMPLETED;
                    coreState.isRunning = false;
                    simulationState.blockingPolicy = BlockingPolicy.NONE;
                }
                else
                    simulationState.blockingPolicy = BlockingPolicy.WRITE;
                break;
        }

        // Add task back to queue if task is not finished but runtime is over
        if (task.stage != Stage.COMPLETED) {
            if (coreState.isRunning)
                coreState.currentTask = task;
            else {
                queueInCore.add(task);
                if (simulationState.blockingPolicy == BlockingPolicy.READ)
                    simulationState.blockingPolicy = BlockingPolicy.NONE;
            }
        } else {
            logger.info("Task " + task.id + " completed at time " + (time + 1) + " with RT " + (time - task.readReleaseTime + 1));
            WCRTInCore.set(task.index, Math.max(WCRTInCore.get(task.index), time - task.readReleaseTime + 1));
        }
    }

    private List<List<Double>> initializeWCRTs(List<Core> cores) {
        List<List<Double>> WCRTs = new ArrayList<>();
        for (Core core: cores) {
            WCRTs.add(new ArrayList<>(Collections.nCopies(core.tasks.size(), 0.0)));
        }
        return WCRTs;
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
        boolean isAdded = false;
        for (Core core : cores) {
            for (Task task : core.tasks) {
                if (!isAdded && (initialJobs(time, task) || periodicJobs(time, task))) {
                    logger.info("\nTasks Released:");
                    isAdded = true;
                }

                if (initialJobs(time, task) || periodicJobs(time, task)) {
                    logger.info("- Task " + task.id + " (Read Time: " + task.readTime + ", Body Time: " + task.bodyTime + ", Write Time: " + task.writeTime + ")");
                    task.readReleaseTime = time;
                    skipReadStageIfNoReadTime(task);
                    queues.get(core.id-1).add(task.copy());
                }
            }
        }
        if (isAdded)
            logger.info("");
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

    private void pathDiverges(int coreIndex, List<Task> minRuntimeTasks, List<Core> cores, List<Queue<Task>> queues, List<List<Double>> WCRTs, CFSSimulationState simulationState, int time, int hyperperiod) {
        List<List<List<Double>>> possibleWCRTs = new ArrayList<>();
        for (int i = 0; i < minRuntimeTasks.size(); i++)
            possibleWCRTs.add(simulatePath(cores, queues, WCRTs, simulationState, time, hyperperiod, minRuntimeTasks, i, coreIndex));

        for (int i = 0; i < WCRTs.size(); i++) {
            List<Double> WCRTInCore = WCRTs.get(i);
            for (int j = 0; j < WCRTInCore.size(); j++) {
                double maxWCRT = 0;
                for (List<List<Double>> wcrts : possibleWCRTs) {
                    maxWCRT = Math.max(maxWCRT, wcrts.get(i).get(j));
                }
                WCRTInCore.set(j, maxWCRT);
            }
        }
    }

    private void setRuntime(int coreIndex, Task task, Queue<Task> queueInCore, CFSSimulationState simulationState) {
        CoreState coreState = simulationState.coreStates.get(coreIndex);
        double totalWeight = queueInCore.stream().mapToDouble(t -> t.weight).sum();
        coreState.remainingRuntime = (int) Math.max(simulationState.targetedLatency * task.weight / totalWeight, simulationState.minimumGranularity);
        coreState.isRunning = true;
    }

    private static List<Task> getMinRuntimeTasks(Queue<Task> queueInCore, CFSSimulationState simulationState) {
        if (queueInCore.isEmpty())
            return new ArrayList<>();

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

                if (!queueInCore.isEmpty()) {
                    minRuntime = queueInCore.peek().virtualRuntime;
                    while (queueInCore.peek().virtualRuntime == minRuntime)
                        minRuntimeTasks.add(queueInCore.poll());
                }

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

                if (!queueInCore.isEmpty()) {
                    minRuntime = queueInCore.peek().virtualRuntime;
                    while (queueInCore.peek().virtualRuntime == minRuntime)
                        minRuntimeTasks.add(queueInCore.poll());
                }

                queueInCore.addAll(writeTasks);
                break;
        }
        return minRuntimeTasks;
    }

    /**
     * 주어진 minRuntimeTask에 대해 runtime을 계산한다.
     * 나머지 코어에 대해서 task를 고르고 coreState에 저장하고 runtime을 계산한다.
     * 나머지 시뮬레이션을 동일하게 진행하고, WCRTs를 업데이트한다.
     */
    private List<List<Double>> simulatePath(List<Core> cores, List<Queue<Task>> queues, List<List<Double>> WCRTs, CFSSimulationState simulationState, int time, int hyperperiod, List<Task> minRuntimeTasks, int taskIndex, int coreIndex) {
        logger.info("\n*** Path diverged ***");

        List<Task> cloneMinRuntimeTasks = new ArrayList<>();
        for (Task task : minRuntimeTasks)
            cloneMinRuntimeTasks.add(task.copy());
        Task minRuntimeTask = cloneMinRuntimeTasks.remove(taskIndex);

        CFSSimulationState cloneSimulationState = simulationState.copy();
        List<Queue<Task>> cloneQueues = copyQueues(queues);
        cloneQueues.get(coreIndex).addAll(cloneMinRuntimeTasks);
        List<List<Double>> cloneWCRTs = WCRTs.stream()
                .map(ArrayList::new)
                .collect(Collectors.toList());
        Queue<Task> cloneQueue = cloneQueues.get(coreIndex);
        List<Double> cloneWCRT = cloneWCRTs.get(coreIndex);
        CoreState cloneCoreState = cloneSimulationState.coreStates.get(coreIndex);

        setRuntime(coreIndex, minRuntimeTask, cloneQueue, cloneSimulationState);
        executeTask(minRuntimeTask, cloneQueue, cloneWCRT, cloneSimulationState, cloneCoreState, time);
        time++;

        performSimulation(cores, cloneQueues, cloneWCRTs, cloneSimulationState, time, hyperperiod);
        checkSchedulability(cores, cloneQueues, cloneWCRTs);
        return cloneWCRTs;
    }

    private List<Queue<Task>> copyQueues(List<Queue<Task>> originalQueues) {
        List<Queue<Task>> newQueues = new ArrayList<>();

        for (Queue<Task> originalQueueInCore : originalQueues) {
            Queue<Task> newQueueInCore = new PriorityQueue<>(Comparator.comparingDouble(task -> task.virtualRuntime));
            for (Task task : originalQueueInCore) {
                newQueueInCore.add(task.copy());
            }
            newQueues.add(newQueueInCore);
        }

        return newQueues;
    }


    private SimulationResult checkSchedulability(List<Core> cores, List<Queue<Task>> queues, List<List<Double>> WCRTs) {
        boolean schedulability = true;

        logger.info("\n------------------------------");
        logger.info("***** Simulation Results *****");
        logger.info("------------------------------");

        logger.info("Unfinished tasks");
        for (int i = 0; i < queues.size(); i++) {
            Queue<Task> queue = queues.get(i);
            logger.info("- Core " + (i+1) + ": " + queue.stream().map(task -> task.id).collect(Collectors.toList()));
            schedulability = false;
        }

        for (int i = 0; i < cores.size(); i++) {
            logger.info("\nCore " + (i+1) + " Results");
            for (int j = 0; j < cores.get(i).tasks.size(); j++) {
                Task task = cores.get(i).tasks.get(j);
                logger.info("- Task " + task.id + " (WCRT: " + WCRTs.get(i).get(j) + ", Period: " + task.period + ")");
                if (WCRTs.get(i).get(j) > task.period)
                    schedulability = false;
            }
        }
        return new SimulationResult(schedulability, WCRTs);
    }

}
