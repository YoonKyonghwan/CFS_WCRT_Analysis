package org.cap.simulation;

import org.cap.model.*;
import org.cap.utility.LoggerUtility;
import org.cap.utility.MathUtility;

import java.util.*;
import java.util.logging.*;
import java.util.stream.Collectors;
import java.util.Map.Entry;

public class PFSSimulator {
    private static final Logger logger = LoggerUtility.getLogger();

    /**
     * This method starts the PFS simulation, initializes the simulation state, and the queue.
     * Then, it performs the simulation and displays the result.
     *
     * @return WCRT - list of worst case response times
     */
    public SimulationResult simulatePFS(List<Core> cores, String logger_option) {
        LoggerUtility.initializeLogger(logger_option);
        logger.info("Starting PFS simulation");

        HashMap<Integer, Long> wcrtMap = initializeWCRTs(cores);
        List<Queue<TaskStat>> queues = initializeQueues(cores);
        PFSSimulationState simulationState = new PFSSimulationState(BlockingPolicy.NONE, "-1:0");
        int time = 0;

        performSimulation(cores, queues, wcrtMap, simulationState, time);

        LoggerUtility.addConsoleLogger();
        return checkSchedulability(cores, queues, wcrtMap);
    }

    /**
     * This method performs the simulation while the current time is less than the LCM of the tasks.
     * It calculates the allocation for each task and executes it accordingly.
     */
    
    private void performSimulation(List<Core> cores, List<Queue<TaskStat>> queues, HashMap<Integer, Long> WCRTs, PFSSimulationState simulationState, int time) {
        long hyperperiod = MathUtility.getLCM(cores);
        while (time < hyperperiod) {
            logger.info(String.format("\n>>> CURRENT TIME: %d <<<\n", time));
            List<List<TaskStat>> runningTasks = initializeRunningTasks(queues, simulationState, time);

            if (runningTasks.isEmpty()) {
                time++;
                continue;
            }

            // Share the CPU proportional to priority weight
            for (int i = 0; i < cores.size(); i++) {
                List<TaskStat> runningTasksInCore = runningTasks.get(i);
                double totalWeight = runningTasksInCore.stream().mapToDouble(t -> t.task.weight).sum();
                for (TaskStat task : runningTasksInCore) {
                    double allocation = 1.0 * (task.task.weight / totalWeight);
                    executeTask(task, allocation, queues.get(i), WCRTs, simulationState, time);
                }
            }

            time++;
            if (time != hyperperiod)
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
    private HashMap<Integer, Long> simulatePath(List<Core> cores, List<Queue<TaskStat>> queues, HashMap<Integer, Long> WCRTs, int time, PFSSimulationState simulationState) {
        logger.info("\n******* Path diverged *******");

        List<Queue<TaskStat>> cloneQueues = copyQueues(queues);
        HashMap<Integer, Long> cloneWCRTs = cloneHashMap(WCRTs);

        performSimulation(cores, cloneQueues, cloneWCRTs, simulationState, time);

        checkSchedulability(cores, cloneQueues, cloneWCRTs);
        return cloneWCRTs;
    }

    private HashMap<Integer, Long> cloneHashMap(HashMap<Integer, Long> mapToBeCopied) {
        HashMap<Integer, Long> clonedMap = new HashMap<Integer, Long>();

        for (Integer key :  mapToBeCopied.keySet()) {
            clonedMap.put(key, Long.valueOf(mapToBeCopied.get(key).longValue()));
        }

        return clonedMap;
    }


    /**
     * This method executes a task according to its current stage.
     * The task can be at the READ, BODY, or WRITE stage.
     * If the task is completed, it is removed from the queue, and its response time is calculated.
     */
    private void executeTask(TaskStat task, double allocation, Queue<TaskStat> queueInCore, HashMap<Integer, Long> WCRTs, PFSSimulationState simulationState, int time) {
        logger.info("Task " + task.task.id + " executed for " + allocation + " in stage: " + task.stage);

        switch (task.stage) {
            case READ:
                task.task.readTime -= allocation;
                if (MathUtility.withinTolerance(task.task.readTime, 0)) {
                    task.stage = Stage.BODY;
                    task.bodyReleaseTime = time + 1;
                }
                else {
                    if (simulationState.blockingPolicy == BlockingPolicy.NONE) {
                        simulationState.blockingPolicy = BlockingPolicy.READ;
                    }
                }
                break;
            case BODY:
                task.task.bodyTime -= allocation;
                if (MathUtility.withinTolerance(task.task.bodyTime, 0)) {
                    if (task.task.writeTime > 0) {
                        task.stage = Stage.WRITE;
                        task.writeReleaseTime = time + 1;
                    }
                    else
                        task.stage = Stage.COMPLETED;
                }
                break;
            case WRITE:
                task.task.writeTime -= allocation;
                if (MathUtility.withinTolerance(task.task.writeTime, 0)) {
                    task.stage = Stage.COMPLETED;
                    simulationState.writingTaskKey = "-1:0";
                }
                else {
                    if (simulationState.blockingPolicy == BlockingPolicy.NONE) {
                        simulationState.blockingPolicy = BlockingPolicy.WRITE;
                        simulationState.writingTaskKey = String.format("%s:%s", task.task.id, task.readReleaseTime);
                    }
                }
                break;
            default:
                break;
        }

        if (task.stage != Stage.COMPLETED) {
            queueInCore.add(task);
        } else {
            // TODO save RT of all jobs at the end
            logger.info("Task " + task.task.id + " completed at time " + (time + 1) + " with RT " + (time - task.readReleaseTime + 1));
            WCRTs.put(Integer.valueOf(task.getId()), Math.max(WCRTs.get(Integer.valueOf(task.getId())), time - task.readReleaseTime + 1));
        }
    }

    /**
     * This method checks if the simulation path diverges.
     * If it does, it simulates each possible path and stores the worst case response time for each.
     *
     * @return boolean - returns true if the path diverges, false otherwise
     */
    private boolean pathDiverges(List<Core> cores, List<Queue<TaskStat>> queues, HashMap<Integer, Long> WCRTs, PFSSimulationState simulationState, int time) {
        List<TaskStat> allTasks = queues.stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

        TaskStat earliestReadTask = allTasks.stream()
            .filter(task -> task.stage == Stage.READ && task.task.startTime <= time)
            .min(Comparator.comparingLong(task -> task.readReleaseTime))
            .orElse(null);

        TaskStat earliestWriteTask = allTasks.stream()
            .filter(task -> task.stage == Stage.WRITE && task.task.startTime <= time)
            .min(Comparator.comparingLong(task -> task.writeReleaseTime))
            .orElse(null);

        List<TaskStat> earliestReadTasks = new ArrayList<>();
        List<TaskStat> earliestWriteTasks = new ArrayList<>();

        if ((earliestReadTask != null) && ((earliestWriteTask == null) || (earliestReadTask.readReleaseTime < earliestWriteTask.writeReleaseTime))) {
            earliestReadTasks = allTasks.stream()
                .filter(task -> task.stage == Stage.READ && task.task.startTime <= time && task.readReleaseTime == earliestReadTask.readReleaseTime)
                .collect(Collectors.toList());
            earliestWriteTasks = allTasks.stream()
                .filter(task -> task.stage == Stage.WRITE && task.task.startTime <= time && task.writeReleaseTime == earliestReadTask.readReleaseTime)
                .collect(Collectors.toList());
        } else if ((earliestWriteTask != null) && ((earliestReadTask == null) || (earliestWriteTask.writeReleaseTime <= earliestReadTask.readReleaseTime))) {
            earliestReadTasks = allTasks.stream()
                .filter(task -> task.stage == Stage.READ && task.task.startTime <= time && task.readReleaseTime == earliestWriteTask.writeReleaseTime)
                .collect(Collectors.toList());
            earliestWriteTasks = allTasks.stream()
                .filter(task -> task.stage == Stage.WRITE && task.task.startTime <= time && task.writeReleaseTime == earliestWriteTask.writeReleaseTime)
                .collect(Collectors.toList());
        }

        List<HashMap<Integer, Long> > possibleWCRTs = new ArrayList<>();

        // Case 1: one or more read and write tasks exist with the same earliest release time
        if (!earliestReadTasks.isEmpty() && !earliestWriteTasks.isEmpty()) {
            possibleWCRTs.add(simulatePath(cores, queues, WCRTs, time, new PFSSimulationState(BlockingPolicy.READ, simulationState.writingTaskKey)));
            for (TaskStat writeTask : earliestWriteTasks) {
                possibleWCRTs.add(simulatePath(cores, queues, WCRTs, time, new PFSSimulationState(BlockingPolicy.WRITE, String.format("%s:%s", writeTask.task.id, writeTask.readReleaseTime))));
            }

            for (Entry<Integer, Long> entry : WCRTs.entrySet()) {
                long maxWCRT = 0;
                for (HashMap<Integer, Long> possibleWcrtMap : possibleWCRTs) {
                    maxWCRT = Math.max(maxWCRT, possibleWcrtMap.get(entry.getKey()).longValue());
                }
                WCRTs.put(entry.getKey(), Long.valueOf(maxWCRT));
            }
            return true;
        }
        // Case 2: multiple write tasks exist with the same earliest release time
        else if (earliestWriteTasks.size() > 1) {
            for (TaskStat writeTask : earliestWriteTasks) {
                possibleWCRTs.add(simulatePath(cores, queues, WCRTs, time, new PFSSimulationState(BlockingPolicy.WRITE, String.format("%s:%s", writeTask.task.id, writeTask.readReleaseTime))));
            }

            for (Entry<Integer, Long> entry : WCRTs.entrySet()) {
                long maxWCRT = 0;
                for (HashMap<Integer, Long> possibleWcrtMap : possibleWCRTs) {
                    maxWCRT = Math.max(maxWCRT, possibleWcrtMap.get(entry.getKey()).longValue());
                }
                WCRTs.put(entry.getKey(), Long.valueOf(maxWCRT));
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
    private List<List<TaskStat>> initializeRunningTasks(List<Queue<TaskStat>> queues, PFSSimulationState simulationState, int time) {
        List<List<TaskStat>> runningTasks = new ArrayList<>();

        // TODO when choosing running tasks, apply FIFO policy
        // If multiple read, write tasks exist, choose based on releaseTime
        // If release times are equal, then diverge path
        for (Queue<TaskStat> queueInCore : queues) {
            // TODO iterate in order of release time
            Iterator<TaskStat> iterator = queueInCore.iterator();
            List<TaskStat> runningTasksInCore = new ArrayList<>();
            while (iterator.hasNext()) {
                TaskStat task = iterator.next();
                if (task.task.startTime > time)
                    continue;

                switch (simulationState.blockingPolicy) {
                    case NONE:
                        // TODO choose read and write task with earliest release time
                        if (task.stage == Stage.READ)
                            simulationState.blockingPolicy = BlockingPolicy.READ;
                        else if (task.stage == Stage.WRITE) {
                            simulationState.blockingPolicy = BlockingPolicy.WRITE;
                            simulationState.writingTaskKey = String.format("%s:%s", task.task.id, task.readReleaseTime);
                        }
                        break;
                    case READ:
                        if (task.stage == Stage.READ || task.stage == Stage.BODY)
                            break;
                        else
                            continue;
                    case WRITE:
                        if (simulationState.writingTaskKey.equals((String.format("%s:%s", task.task.id, task.readReleaseTime))) || task.stage == Stage.BODY)
                            break;
                        else
                            continue;
                }
                runningTasksInCore.add(task);
                iterator.remove();
            }

            runningTasks.add(runningTasksInCore);
            logger.info("Running tasks in core " + runningTasks.size() + ": " + runningTasksInCore.stream().map(task -> task.task.id).collect(Collectors.toList()));
        }

        return runningTasks;
    }

    private HashMap<Integer, Long> initializeWCRTs(List<Core> cores) {
        HashMap<Integer, Long> wcrtMap = new HashMap<Integer, Long>();

        for (Core core: cores) {
            for (Task task : core.tasks) {
                wcrtMap.put(Integer.valueOf(task.getId()), 0L);
            }
        }
        return wcrtMap;
    }


    private List<Queue<TaskStat>> initializeQueues(List<Core> cores) {
        List<Queue<TaskStat>> queues = new ArrayList<>();
        for (Core core : cores) {
            Queue<TaskStat> queueInCore = new PriorityQueue<>(Comparator.comparingDouble(task -> task.readReleaseTime));
            for (Task task : core.tasks) {
                TaskStat taskStat = new TaskStat(task);
                taskStat.readReleaseTime = task.startTime;
                taskStat.virtualRuntime = 0L;
                
                skipReadStageIfNoReadTime(taskStat);
                if (task.startTime == 0L) {
                    queueInCore.add(taskStat);
                }
            }
            queues.add(queueInCore);
        }
        return queues;
    }

    private List<Queue<TaskStat>> copyQueues(List<Queue<TaskStat>> originalQueues) {
        List<Queue<TaskStat>> newQueues = new ArrayList<>();

        for (Queue<TaskStat> originalQueueInCore : originalQueues) {
            Queue<TaskStat> newQueueInCore = new PriorityQueue<>(Comparator.comparingDouble(task -> task.readReleaseTime));
            for (TaskStat task : originalQueueInCore) {
                newQueueInCore.add(task.copy());
            }
            newQueues.add(newQueueInCore);
        }

        return newQueues;
    }


    private void addPeriodicJobs(List<Core> cores, List<Queue<TaskStat>> queues, int time) {
        for (Core core : cores) {
            // TODO add to core in order of release time
            for (Task task : core.tasks) {
                if (time > task.startTime && task.period > 0 && time % task.period == 0) {
                    logger.info("Task " + task.id + " released with read time " + task.readTime + ", body Time " + task.bodyTime + ", write time " + task.writeTime);
                    TaskStat taskStat = new TaskStat(task);
                    taskStat.readReleaseTime = time;
                    skipReadStageIfNoReadTime(taskStat);
                    queues.get(core.coreID-1).add(taskStat.copy());
                }
            }
        }
    }

    private void skipReadStageIfNoReadTime(TaskStat task) {
        if (task.stage == Stage.READ && task.task.readTime <= 0) {
            task.stage = Stage.BODY;
            task.bodyReleaseTime = task.readReleaseTime;
        }
    }

    // TODO check if condition has to change
    private boolean noReadAndWriteTasksRunning(List<List<TaskStat>> runningTasks, BlockingPolicy blockingPolicy) {
        return runningTasks.stream()
                .flatMap(List::stream)
                .noneMatch(task -> task.stage == Stage.READ || task.stage == Stage.WRITE) || blockingPolicy == BlockingPolicy.NONE;
    }

    private SimulationResult checkSchedulability(List<Core> cores, List<Queue<TaskStat>> queues, HashMap<Integer, Long> WCRTs) {
        boolean schedulability = true;

        logger.info("\n******************************");
        logger.info("***** Simulation Results *****");
        logger.info("******************************");

        for (int i = 0; i < queues.size(); i++) {
            Queue<TaskStat> queue = queues.get(i);
            logger.info("Unfinished tasks in core " + (i+1) + ": " + queue.stream().map(task -> task.task.id).collect(Collectors.toList()));
            schedulability = false;
        }

        for (int i = 0; i < cores.size(); i++) {
            logger.info("\n******* Core " + (i+1) + " Results *******");
            for (int j = 0; j < cores.get(i).tasks.size(); j++) {
                Task task = cores.get(i).tasks.get(j);
                logger.info("Task " + task.id + " WCRT: " + WCRTs.get(Integer.valueOf(task.getId())) + " period: " + task.period);
                if (WCRTs.get(Integer.valueOf(task.getId()))  > task.period)
                    schedulability = false;
            }
        }
        return new SimulationResult(schedulability, WCRTs);
    }

}
