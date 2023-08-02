package org.cap.simulation;

import org.cap.model.*;
import org.cap.utility.LoggerUtility;
import org.cap.utility.MathUtility;

import java.util.*;
import java.util.logging.*;
import java.util.stream.Collectors;

public class CFSSimulator {
    private static final Logger logger = LoggerUtility.getLogger();

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

    /**
     * This method starts the CFS simulation, initializes the simulation state, and the queue.
     * Then, it performs the simulation and displays the result.
     *
     * @return WCRT - list of worst case response times
     */
    public List<List<Double>> simulateCFS(List<Core> cores) {
        LoggerUtility.initializeLogger();
        logger.info("Starting CFS simulation");

        List<List<Double>> WCRTs = initializeWCRTs(cores);
        List<Queue<Task>> queues = initializeQueues(cores);
        SimulationState simulationState = new SimulationState(BlockingPolicy.NONE, "-1:0");
        int time = 0;

        performSimulation(cores, WCRTs, queues, simulationState, time);

        LoggerUtility.addConsoleLogger();
        displayResult(WCRTs, queues);
        return WCRTs;
    }

    /**
     * This method performs the simulation while the current time is less than the LCM of the tasks.
     * It calculates the allocation for each task and executes it accordingly.
     */
    private void performSimulation(List<Core> cores, List<List<Double>> WCRTs, List<Queue<Task>> queues, SimulationState simulationState, int time) {
        while (time < MathUtility.getLCM(cores)) {
            logger.info(String.format("\n>>> CURRENT TIME: %d <<<\n", time));
            List<List<Task>> runningTasks = initializeRunningTasks(queues, simulationState, time);

            if (runningTasks.isEmpty()) {
                time++;
                continue;
            }

            // Share the CPU proportional to priority weight
            for (int i = 0; i < cores.size(); i++) {
                List<Task> runningTasksInCore = runningTasks.get(i);
                double totalPriorityWeight = runningTasksInCore.stream().mapToDouble(t -> t.priorityWeight).sum();
                for (Task task : runningTasksInCore) {
                    double allocation = 1.0 * (task.priorityWeight / totalPriorityWeight);
                    executeTask(task, allocation, queues.get(i), WCRTs.get(i), simulationState, time);
                }
            }

            time += 1;
            addPeriodicJobs(cores, queues, time);

            if (noReadAndWriteTasksRunning(runningTasks, simulationState.blockingPolicy)) {
                simulationState.blockingPolicy = BlockingPolicy.NONE;
                if (pathDiverges(cores, queues, WCRTs, simulationState, time)) break;
            }
            logger.info("Blocking policy " + simulationState.blockingPolicy);
        }
    }

    /**
     * This method simulates a path, which means it creates a clone of the queue and WCRT,
     * performs the simulation using the clones, and displays the result.
     *
     * @return cloneWCRT - list of worst case response times for the simulated path
     */
    private List<List<Double>> simulatePath(List<Core> cores, List<Queue<Task>> queues, List<List<Double>> WCRTs, int time, SimulationState simulationState) {
        logger.info("\n******* Path diverged *******");

        List<Queue<Task>> cloneQueues = copyQueues(queues);
        List<List<Double>> cloneWCRTs = WCRTs.stream()
                .map(ArrayList::new)
                .collect(Collectors.toList());

        performSimulation(cores, cloneWCRTs, cloneQueues, simulationState, time);

        displayResult(cloneWCRTs, cloneQueues);
        return cloneWCRTs;
    }

    /**
     * This method executes a task according to its current stage.
     * The task can be at the READ, BODY, or WRITE stage.
     * If the task is completed, it is removed from the queue, and its response time is calculated.
     */
    private void executeTask(Task task, double allocation, Queue<Task> queueInCore, List<Double> WCRTInCore, SimulationState simulationState, int time) {
        skipReadStageIfNoReadTime(task);
        logger.info("Task " + task.id + " executed for " + allocation + " in stage: " + task.stage);

        switch (task.stage) {
            case READ:
                task.readTime -= allocation;
                if (MathUtility.withinTolerance(task.readTime, 0)) {
                    task.stage = Stage.BODY;
                }
                else {
                    if (simulationState.blockingPolicy == BlockingPolicy.NONE) {
                        simulationState.blockingPolicy = BlockingPolicy.READ;
                    }
                }
                break;
            case BODY:
                task.bodyTime -= allocation;
                if (MathUtility.withinTolerance(task.bodyTime, 0)) {
                    if (task.writeTime > 0)
                        task.stage = Stage.WRITE;
                    else
                        task.stage = Stage.COMPLETED;
                }
                break;
            case WRITE:
                task.writeTime -= allocation;
                if (MathUtility.withinTolerance(task.writeTime, 0)) {
                    task.stage = Stage.COMPLETED;
                    simulationState.writingTaskKey = "-1:0";
                }
                else {
                    if (simulationState.blockingPolicy == BlockingPolicy.NONE) {
                        simulationState.blockingPolicy = BlockingPolicy.WRITE;
                        simulationState.writingTaskKey = String.format("%s:%s", task.id, task.releaseTime);
                    }
                }
                break;
        }

        if (task.stage != Stage.COMPLETED) {
            queueInCore.add(task);
        } else {
            // TODO save RT of all jobs at the end
            logger.info("Task " + task.id + " completed at time " + (time + 1) + " with RT " + (time - task.releaseTime + 1));
            WCRTInCore.set(task.index, Math.max(WCRTInCore.get(task.index), time - task.releaseTime + 1));
        }
    }

    /**
     * This method checks if the simulation path diverges.
     * If it does, it simulates each possible path and stores the worst case response time for each.
     *
     * @return boolean - returns true if the path diverges, false otherwise
     */
    private boolean pathDiverges(List<Core> cores, List<Queue<Task>> queues, List<List<Double>> WCRTs, SimulationState simulationState, int time) {
        List<Task> allTasks = queues.stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        Task earliestTask = allTasks.stream()
                .filter(task -> task.startTime <= time)
                .min(Comparator.comparingInt(task -> task.releaseTime))
                .orElse(null);

        List<Task> earliestReadTasks = allTasks.stream()
                .filter(task -> task.stage == Stage.READ && task.startTime <= time && task.releaseTime == earliestTask.releaseTime)
                .collect(Collectors.toList());

        List<Task> earliestWriteTasks = allTasks.stream()
                .filter(task -> task.stage == Stage.WRITE && task.startTime <= time && task.releaseTime == earliestTask.releaseTime)
                .collect(Collectors.toList());

        List<List<List<Double>>> possibleWCRTs = new ArrayList<>();

        // Case 1: one or more read and write tasks exist with the same earliest release time
        if (!earliestReadTasks.isEmpty() && !earliestWriteTasks.isEmpty()) {
            possibleWCRTs.add(simulatePath(cores, queues, WCRTs, time, new SimulationState(BlockingPolicy.READ, simulationState.writingTaskKey)));
            for (Task writeTask : earliestWriteTasks) {
                possibleWCRTs.add(simulatePath(cores, queues, WCRTs, time, new SimulationState(BlockingPolicy.WRITE, String.format("%s:%s", writeTask.id, writeTask.releaseTime))));
            }

            for (int i = 0; i < WCRTs.size(); i++) {
                List<Double> WCRTInCore = WCRTs.get(i);
                for (int k = 0; k < WCRTInCore.size(); k++) {
                    double maxWCRT = 0;
                    for (List<List<Double>> wcrts : possibleWCRTs) {
                        maxWCRT = Math.max(maxWCRT, wcrts.get(i).get(k));
                    }
                    WCRTInCore.set(k, maxWCRT);
                }
            }
            return true;
        }
        // Case 2: multiple write tasks exist with the same earliest release time
        else if (earliestWriteTasks.size() > 1) {
            for (Task writeTask : earliestWriteTasks) {
                possibleWCRTs.add(simulatePath(cores, queues, WCRTs, time, new SimulationState(BlockingPolicy.WRITE, String.format("%s:%s", writeTask.id, writeTask.releaseTime))));
            }

            for (int i = 0; i< WCRTs.size(); i++) {
                List<Double> WCRTInCore = WCRTs.get(i);
                for (int k = 0; k< WCRTInCore.size(); k++) {
                    double maxWCRT = 0;
                    for (List<List<Double>> wcrts : possibleWCRTs) {
                        maxWCRT = Math.max(maxWCRT, wcrts.get(i).get(k));
                    }
                    WCRTInCore.set(k, maxWCRT);
                }
            }
            return true;
        }
        return false;
    }

    /**
     * This method initializes the list of running tasks based on the blocking policy.
     * If the task has been released, depending on the blocking policy, the task may be added to running tasks.
     * It iterates through the queue and removes tasks that are added to the running tasks list.
     *
     * @return runningTasks - list of tasks that are currently running
     */
    private List<List<Task>> initializeRunningTasks(List<Queue<Task>> queues, SimulationState simulationState, int time) {
        List<List<Task>> runningTasks = new ArrayList<>();

        // TODO when choosing running tasks, apply FIFO policy
        // If multiple read, write tasks exist, choose based on releaseTime
        // If release times are equal, then diverge path
        for (Queue<Task> queueInCore : queues) {
            // TODO iterate in order of release time
            Iterator<Task> iterator = queueInCore.iterator();
            List<Task> runningTasksInCore = new ArrayList<>();
            while (iterator.hasNext()) {
                Task task = iterator.next();
                if (task.startTime > time)
                    continue;

                switch (simulationState.blockingPolicy) {
                    case NONE:
                        // TODO choose read and write task with earliest release time
                        if (task.stage == Stage.READ)
                            simulationState.blockingPolicy = BlockingPolicy.READ;
                        else if (task.stage == Stage.WRITE) {
                            simulationState.blockingPolicy = BlockingPolicy.WRITE;
                            simulationState.writingTaskKey = String.format("%s:%s", task.id, task.releaseTime);
                        }
                        break;
                    case READ:
                        if (task.stage == Stage.READ || task.stage == Stage.BODY)
                            break;
                        else
                            continue;
                    case WRITE:
                        if (simulationState.writingTaskKey.equals((String.format("%s:%s", task.id, task.releaseTime))) || task.stage == Stage.BODY)
                            break;
                        else
                            continue;
                }
                runningTasksInCore.add(task);
                iterator.remove();
            }

            runningTasks.add(runningTasksInCore);
            logger.info("Running tasks in core " + runningTasks.size() + ": " + runningTasksInCore.stream().map(task -> task.id).collect(Collectors.toList()));
        }

        return runningTasks;
    }

    private static List<List<Double>> initializeWCRTs(List<Core> cores) {
        List<List<Double>> WCRTs = new ArrayList<>();
        for (Core core: cores) {
            WCRTs.add(new ArrayList<>(Collections.nCopies(core.tasks.size(), 0.0)));
        }
        return WCRTs;
    }

    private List<Queue<Task>> initializeQueues(List<Core> cores) {
        List<Queue<Task>> queues = new ArrayList<>();
        for (Core core : cores) {
            Queue<Task> queueInCore = new PriorityQueue<>(Comparator.comparingDouble(task -> task.priorityWeight));
            // TODO add to queue in order of release time
            for (Task task : core.tasks) {
                task.priorityWeight = priorityToWeight.get(task.nice + 20);
                task.originalReadTime = task.readTime;
                task.originalBodyTime = task.bodyTime;
                task.originalWriteTime = task.writeTime;
                task.releaseTime = task.startTime;
                queueInCore.add(task.copy());
            }
            queues.add(queueInCore);
        }
        return queues;
    }

    private List<Queue<Task>> copyQueues(List<Queue<Task>> originalQueues) {
        List<Queue<Task>> newQueues = new ArrayList<>();

        for (Queue<Task> originalQueueInCore : originalQueues) {
            Queue<Task> newQueueInCore = new PriorityQueue<>(Comparator.comparingDouble(task -> task.priorityWeight));
            for (Task task : originalQueueInCore) {
                newQueueInCore.add(task.copy());
            }
            newQueues.add(newQueueInCore);
        }

        return newQueues;
    }


    private void addPeriodicJobs(List<Core> cores, List<Queue<Task>> queues, int time) {
        for (Core core : cores) {
            // TODO add to core in order of release time
            for (Task task : core.tasks) {
                if (time > task.startTime && task.period > 0 && time % task.period == 0) {
                    task.releaseTime = time;
                    queues.get(core.id-1).add(task.copy());
                    logger.info("Task " + task.id + " released with read time " + task.readTime + ", write time " + task.writeTime + ", body Time " + task.bodyTime);
                }
            }
        }
    }

    private void skipReadStageIfNoReadTime(Task task) {
        if (task.stage == Stage.READ && task.readTime <= 0) {
            task.stage = Stage.BODY;
        }
    }

    private void displayResult(List<List<Double>> WCRTs, List<Queue<Task>> queues) {
        logger.info("\n******************************");
        logger.info("***** Simulation Results *****");
        logger.info("******************************");

        for (int i = 0; i < queues.size(); i++) {
            Queue<Task> queue = queues.get(i);
            logger.info("Unfinished tasks in core " + (i+1) + ": " + queue.stream().map(task -> task.id).collect(Collectors.toList()));
        }

        for (int i = 0; i < WCRTs.size(); i++) {
            logger.info("Task " + (i+1) + " WCRT: " + WCRTs.get(i));
        }
    }

    private boolean noReadAndWriteTasksRunning(List<List<Task>> runningTasks, BlockingPolicy blockingPolicy) {
        return runningTasks.stream()
                .flatMap(List::stream)
                .noneMatch(task -> task.stage == Stage.READ || task.stage == Stage.WRITE) || blockingPolicy == BlockingPolicy.NONE;
    }
}
