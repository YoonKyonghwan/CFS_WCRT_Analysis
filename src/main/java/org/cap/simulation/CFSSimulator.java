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

    // TODO support multi-core
    // group tasks by core in json
    // iterate through each core and calculate allocation
    // all cores share shared resource, so simulation state is single

    // TODO some refactor points
    // TODO maybe add runningTasks, queue, WCRT to core? instead of passing all by list
    //      or use List<List<Task>> instead of List<Core>
    // TODO make WCRT list of WCRT to show results core by core
    // TODO rename variables to be more clear and understandable

    // TODO update test cases to support multi-core

    /**
     * This method starts the CFS simulation, initializes the simulation state, and the queue.
     * Then, it performs the simulation and displays the result.
     *
     * @return WCRT - list of worst case response times
     */
    public List<List<Double>> simulateCFS(List<Core> cores) {
        LoggerUtility.initializeLogger();
        logger.info("Starting CFS simulation");

        // TODO change tasks to cores
        // TODO WCRT needs to be made of all tasks in all cores
        // TODO are core number of queues needed? or is just one queue with all tasks fine?
        // TODO rename variables to be more clear
        List<List<Double>> WCRT = new ArrayList<>();
        for (Core core: cores) {
            WCRT.add(new ArrayList<>(Collections.nCopies(core.tasks.size(), 0.0)));
        }
        SimulationState simulationState = new SimulationState(BlockingPolicy.NONE, "-1:0");
        List<Queue<Task>> queues = initializeQueues(cores);
        int time = 0;

        // TODO pass cores instead of tasks
        // should I use List<Core> or List<List<Task>>?
        // consider adding queue and runningTasks to core?
        performSimulation(cores, WCRT, simulationState, time, queues);

        LoggerUtility.addConsoleLogger();
        displayResult(WCRT, queues);
        return WCRT;
    }

    /**
     * This method performs the simulation while the current time is less than the LCM of the tasks.
     * It calculates the allocation for each task and executes it accordingly.
     */
    private void performSimulation(List<Core> cores, List<List<Double>> WCRT, SimulationState simulationState, int time, List<Queue<Task>> queues) {
        while (time < MathUtility.getLCM(cores)) {
            logger.info(String.format("\n>>> CURRENT TIME: %d <<<\n", time));
            // TODO initialize running tasks for each core
            List<List<Task>> runningTasks = initializeRunningTasks(queues, simulationState, time);

            if (runningTasks.isEmpty()) {
                time++;
                continue;
            }

            // TODO iterate through cores
            // Share the CPU proportional to priority weight
            for (int i = 0; i < cores.size(); i++) {
                List<Task> coreTasks = runningTasks.get(i);
                double totalPriorityWeight = coreTasks.stream().mapToDouble(t -> t.priorityWeight).sum();
                for (Task task : coreTasks) {
                    double allocation = 1.0 * (task.priorityWeight / totalPriorityWeight);
                    executeTask(task, allocation, queues.get(i), WCRT.get(i), simulationState, time);
                }
            }

            time += 1;
            // TODO add periodic jobs to each queue
            addPeriodicJobs(cores, queues, time);

            if (noReadAndWriteTasksRunning(runningTasks, simulationState.blockingPolicy)) {
                simulationState.blockingPolicy = BlockingPolicy.NONE;
                if (pathDiverges(cores, queues, WCRT, simulationState, time)) break;
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
    private List<List<Double>> simulatePath(List<Core> cores, List<Queue<Task>> queues, List<List<Double>> WCRT, int time, SimulationState simulationState) {
        logger.info("\n******* Path diverged *******");

        List<Queue<Task>> cloneQueues = copyQueues(queues);
        List<List<Double>> cloneWCRT = WCRT.stream()
                .map(ArrayList::new)
                .collect(Collectors.toList());

        performSimulation(cores, cloneWCRT, simulationState, time, cloneQueues);

        displayResult(cloneWCRT, cloneQueues);
        return cloneWCRT;
    }

    /**
     * This method executes a task according to its current stage.
     * The task can be at the READ, BODY, or WRITE stage.
     * If the task is completed, it is removed from the queue, and its response time is calculated.
     */
    private void executeTask(Task currentTask, double allocation, Queue<Task> queue, List<Double> WCRT, SimulationState simulationState, int time) {
        skipReadStageIfNoReadTime(currentTask);
        logger.info("Task " + currentTask.id + " executed for " + allocation + " | stage: " + currentTask.stage);

        switch (currentTask.stage) {
            case READ:
                currentTask.readTime -= allocation;
                if (MathUtility.withinTolerance(currentTask.readTime, 0)) {
                    currentTask.stage = Stage.BODY;
                }
                else {
                    if (simulationState.blockingPolicy == BlockingPolicy.NONE) {
                        simulationState.blockingPolicy = BlockingPolicy.READ;
                    }
                }
                break;
            case BODY:
                currentTask.bodyTime -= allocation;
                if (MathUtility.withinTolerance(currentTask.bodyTime, 0)) {
                    if (currentTask.writeTime > 0)
                        currentTask.stage = Stage.WRITE;
                    else
                        currentTask.stage = Stage.COMPLETED;
                }
                break;
            case WRITE:
                currentTask.writeTime -= allocation;
                if (MathUtility.withinTolerance(currentTask.writeTime, 0)) {
                    currentTask.stage = Stage.COMPLETED;
                    simulationState.writingTaskKey = "-1:0";
                }
                else {
                    if (simulationState.blockingPolicy == BlockingPolicy.NONE) {
                        simulationState.blockingPolicy = BlockingPolicy.WRITE;
                        simulationState.writingTaskKey = String.format("%s:%s", currentTask.id, currentTask.currentPeriodStart);
                    }
                }
                break;
        }

        if (currentTask.stage != Stage.COMPLETED) {
            queue.add(currentTask);
        } else {
            // TODO save RT of all jobs at the end
            logger.info("Task " + currentTask.id + " completed at time " + (time + 1) + " with RT " + (time - currentTask.currentPeriodStart + 1));
            WCRT.set(currentTask.index, Math.max(WCRT.get(currentTask.index), time - currentTask.currentPeriodStart + 1));
        }
    }

    /**
     * This method checks if the simulation path diverges.
     * If it does, it simulates each possible path and stores the worst case response time for each.
     *
     * @return boolean - returns true if the path diverges, false otherwise
     */
    private boolean pathDiverges(List<Core> cores, List<Queue<Task>> queues, List<List<Double>> WCRT, SimulationState simulationState, int time) {
        List<Task> allTasks = queues.stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        List<Task> readTasks = allTasks.stream()
                .filter(task -> task.stage == Stage.READ && task.startTime <= time)
                .collect(Collectors.toList());

        List<Task> writeTasks = allTasks.stream()
                .filter(task -> task.stage == Stage.WRITE && task.startTime <= time)
                .collect(Collectors.toList());

        List<List<List<Double>>> possibleWCRT = new ArrayList<>();

        // Case 1: read and write tasks exist
        if (!readTasks.isEmpty() && !writeTasks.isEmpty()) {
            possibleWCRT.add(simulatePath(cores, queues, WCRT, time, new SimulationState(BlockingPolicy.READ, simulationState.writingTaskKey)));
            for (Task writeTask : writeTasks) {
                possibleWCRT.add(simulatePath(cores, queues, WCRT, time, new SimulationState(BlockingPolicy.WRITE, String.format("%s:%s", writeTask.id, writeTask.currentPeriodStart))));
            }

            // TODO get maximum WCRT for each task in each core
            for (int i = 0; i < WCRT.size(); i++) {
                List<Double> taskWCRTs = WCRT.get(i);
                for (int k = 0; k < taskWCRTs.size(); k++) {
                    double maxWCRT = 0;
                    for (List<List<Double>> wcrt : possibleWCRT) {
                        maxWCRT = Math.max(maxWCRT, wcrt.get(i).get(k));
                    }
                    taskWCRTs.set(k, maxWCRT);
                }
            }
            return true;
        }
        // Case 2: multiple write tasks exist
        else if (writeTasks.size() > 1) {
            for (Task writeTask : writeTasks) {
                possibleWCRT.add(simulatePath(cores, queues, WCRT, time, new SimulationState(BlockingPolicy.WRITE, String.format("%s:%s", writeTask.id, writeTask.currentPeriodStart))));
            }

            for (int i = 0; i< WCRT.size(); i++) {
                List<Double> taskWCRTs = WCRT.get(i);
                for (int k = 0; k< taskWCRTs.size(); k++) {
                    double maxWCRT = 0;
                    for (List<List<Double>> wcrt : possibleWCRT) {
                        maxWCRT = Math.max(maxWCRT, wcrt.get(i).get(k));
                    }
                    taskWCRTs.set(k, maxWCRT);
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

        for (Queue<Task> queue : queues) {
            Iterator<Task> iterator = queue.iterator();
            List<Task> runningTasksInCurrentCore = new ArrayList<>();
            while (iterator.hasNext()) {
                Task task = iterator.next();
                if (task.startTime > time)
                    continue;

                switch (simulationState.blockingPolicy) {
                    case NONE:
                        if (task.stage == Stage.READ)
                            simulationState.blockingPolicy = BlockingPolicy.READ;
                        else if (task.stage == Stage.WRITE) {
                            simulationState.blockingPolicy = BlockingPolicy.WRITE;
                            simulationState.writingTaskKey = String.format("%s:%s", task.id, task.currentPeriodStart);
                        }
                        break;
                    case READ:
                        if (task.stage == Stage.READ || task.stage == Stage.BODY)
                            break;
                        else
                            continue;
                    case WRITE:
                        if (simulationState.writingTaskKey.equals((String.format("%s:%s", task.id, task.currentPeriodStart))) || task.stage == Stage.BODY)
                            break;
                        else
                            continue;
                }
                runningTasksInCurrentCore.add(task);
                iterator.remove();
            }

            runningTasks.add(runningTasksInCurrentCore);
            logger.info("Running tasks in core " + runningTasks.size() + ": " + runningTasksInCurrentCore.stream().map(task -> task.id).collect(Collectors.toList()));
        }

        return runningTasks;
    }

    private List<Queue<Task>> initializeQueues(List<Core> cores) {
        List<Queue<Task>> queues = new ArrayList<>();
        for (Core core : cores) {
            Queue<Task> queue = new PriorityQueue<>(Comparator.comparingDouble(task -> task.priorityWeight));
            for (Task task : core.tasks) {
                task.priorityWeight = priorityToWeight.get(task.nice + 20);
                task.originalReadTime = task.readTime;
                task.originalBodyTime = task.bodyTime;
                task.originalWriteTime = task.writeTime;
                task.currentPeriodStart = task.startTime;
                queue.add(task.copy());
            }
            queues.add(queue);
        }
        return queues;
    }

    private List<Queue<Task>> copyQueues(List<Queue<Task>> originalQueues) {
        List<Queue<Task>> newQueues = new ArrayList<>();

        for (Queue<Task> originalQueue : originalQueues) {
            Queue<Task> newQueue = new PriorityQueue<>(Comparator.comparingDouble(task -> task.priorityWeight));
            for (Task task : originalQueue) {
                newQueue.add(task.copy());
            }
            newQueues.add(newQueue);
        }

        return newQueues;
    }


    private void addPeriodicJobs(List<Core> cores, List<Queue<Task>> queues, int time) {
        for (Core core : cores) {
            for (Task task : core.tasks) {
                if (time > task.startTime && task.period > 0 && time % task.period == 0) {
                    task.currentPeriodStart = time;
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

    private void displayResult(List<List<Double>> WCRT, List<Queue<Task>> queues) {
        logger.info("\n******************************");
        logger.info("***** Simulation Results *****");
        logger.info("******************************");

        for (int i = 0; i < queues.size(); i++) {
            Queue<Task> queue = queues.get(i);
            logger.info("Unfinished tasks in core " + (i+1) + ": " + queue.stream().map(task -> task.id).collect(Collectors.toList()));
        }

        for (int i = 0; i < WCRT.size(); i++) {
            logger.info("Task " + (i+1) + " WCRT: " + WCRT.get(i));
        }
    }

    private boolean noReadAndWriteTasksRunning(List<List<Task>> runningTasks, BlockingPolicy blockingPolicy) {
        return runningTasks.stream()
                .flatMap(List::stream)
                .noneMatch(task -> task.stage == Stage.READ || task.stage == Stage.WRITE) || blockingPolicy == BlockingPolicy.NONE;
    }
}
