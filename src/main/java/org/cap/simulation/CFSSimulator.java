package org.cap.simulation;

import org.cap.model.BlockingPolicy;
import org.cap.model.SimulationState;
import org.cap.model.Stage;
import org.cap.model.Task;
import org.cap.utility.CustomFormatter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.*;
import java.util.stream.Collectors;

public class CFSSimulator {
    private static final Logger logger = Logger.getLogger(CFSSimulator.class.getName());

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
    public ArrayList<Double> simulateCFS(List<Task> tasks) {
        initializelogger();
        logger.info("Starting CFS simulation");

        ArrayList<Double> WCRT = new ArrayList<>(Collections.nCopies(tasks.size(), 0.0));
        SimulationState simulationState = new SimulationState(BlockingPolicy.NONE, "-1:0");
        Queue<Task> queue = new PriorityQueue<>(Comparator.comparingDouble(task -> task.priorityWeight));
        initializeQueue(tasks, queue);
        int time = 0;

        performSimulation(tasks, WCRT, simulationState, time, queue);

        addConsoleLogger();
        displayResult(WCRT, queue);
        return WCRT;
    }

    /**
     * This method performs the simulation while the current time is less than the LCM of the tasks.
     * It calculates the allocation for each task and executes it accordingly.
     */
    private void performSimulation(List<Task> tasks, ArrayList<Double> WCRT, SimulationState simulationState, int time, Queue<Task> queue) {
        while (time < getLCM(tasks)) {
            logger.info(String.format("\n>>> CURRENT TIME: %d <<<\n", time));
            List<Task> runningTasks = initializeRunningTasks(queue, simulationState, time);

            if (runningTasks.isEmpty()) {
                time++;
                continue;
            }

            // Share the CPU proportional to priority weight
            double totalPriorityWeight = runningTasks.stream().mapToDouble(t -> t.priorityWeight).sum();
            for (Task currentTask : runningTasks) {
                double allocation = 1.0 * (currentTask.priorityWeight / totalPriorityWeight);
                executeTask(currentTask, allocation, queue, WCRT, simulationState, time);
            }

            time += 1;
            addPeriodicJobs(tasks, queue, time);

            if (noReadAndWriteTasksRunning(runningTasks, simulationState.blockingPolicy)) {
                simulationState.blockingPolicy = BlockingPolicy.NONE;
                if (pathDiverges(tasks, queue, WCRT, simulationState, time)) break;
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
    private ArrayList<Double> simulatePath(List<Task> tasks, Queue<Task> queue, ArrayList<Double> WCRT, int time, SimulationState simulationState) {
        logger.info("\n******* Path diverged *******");

        Queue<Task> cloneQueue = copyQueue(queue);
        ArrayList<Double> cloneWCRT = new ArrayList<>(WCRT);

        performSimulation(tasks, cloneWCRT, simulationState, time, cloneQueue);

        displayResult(cloneWCRT, cloneQueue);
        return cloneWCRT;
    }

    /**
     * This method executes a task according to its current stage.
     * The task can be at the READ, BODY, or WRITE stage.
     * If the task is completed, it is removed from the queue, and its response time is calculated.
     */
    private void executeTask(Task currentTask, double allocation, Queue<Task> queue, ArrayList<Double> WCRT, SimulationState simulationState, int time) {
        skipReadStageIfNoReadTime(currentTask);
        logger.info("Task " + currentTask.id + " executed for " + allocation + " | stage: " + currentTask.stage);

        switch (currentTask.stage) {
            case READ:
                currentTask.readTime -= allocation;
                if (withinTolerance(currentTask.readTime, 0)) {
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
                if (withinTolerance(currentTask.bodyTime, 0)) {
                    if (currentTask.writeTime > 0)
                        currentTask.stage = Stage.WRITE;
                    else
                        currentTask.stage = Stage.COMPLETED;
                }
                break;
            case WRITE:
                currentTask.writeTime -= allocation;
                if (withinTolerance(currentTask.writeTime, 0)) {
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
            WCRT.set(currentTask.id - 1, Math.max(WCRT.get(currentTask.id - 1), time - currentTask.currentPeriodStart + 1));
        }
    }

    /**
     * This method checks if the simulation path diverges.
     * If it does, it simulates each possible path and stores the worst case response time for each.
     *
     * @return boolean - returns true if the path diverges, false otherwise
     */
    private boolean pathDiverges(List<Task> tasks, Queue<Task> queue, ArrayList<Double> WCRT, SimulationState simulationState, int time) {
        List<Task> readTasks = queue.stream().filter(task -> task.stage == Stage.READ && task.startTime <= time).collect(Collectors.toList());
        List<Task> writeTasks = queue.stream().filter(task -> task.stage == Stage.WRITE && task.startTime <= time).collect(Collectors.toList());

        ArrayList<ArrayList<Double>> possibleWCRT = new ArrayList<>();

        // Case 1: read and write tasks exist
        if (!readTasks.isEmpty() && !writeTasks.isEmpty()) {
            possibleWCRT.add(simulatePath(tasks, queue, WCRT, time, new SimulationState(BlockingPolicy.READ, simulationState.writingTaskKey)));
            for (Task writeTask : writeTasks) {
                possibleWCRT.add(simulatePath(tasks, queue, WCRT, time, new SimulationState(BlockingPolicy.WRITE, String.format("%s:%s", writeTask.id, writeTask.currentPeriodStart))));
            }

            for (int i = 0; i< WCRT.size(); i++) {
                double maxWCRT = 0;
                for (int j=0; j<possibleWCRT.size(); j++) {
                    maxWCRT = Math.max(maxWCRT, possibleWCRT.get(j).get(i));
                }
                WCRT.set(i, maxWCRT);
            }
            return true;
        }
        // Case 2: multiple write tasks exist
        else if (writeTasks.size() > 1) {
            for (Task writeTask : writeTasks) {
                possibleWCRT.add(simulatePath(tasks, queue, WCRT, time, new SimulationState(BlockingPolicy.WRITE, String.format("%s:%s", writeTask.id, writeTask.currentPeriodStart))));
            }

            for (int i = 0; i< WCRT.size(); i++) {
                double maxWCRT = 0;
                for (int j=0; j<possibleWCRT.size(); j++) {
                    maxWCRT = Math.max(maxWCRT, possibleWCRT.get(j).get(i));
                }
                WCRT.set(i, maxWCRT);
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
    private List<Task> initializeRunningTasks(Queue<Task> queue, SimulationState simulationState, int time) {
        List<Task> runningTasks = new ArrayList<>();
        Iterator<Task> iterator = queue.iterator();

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
            runningTasks.add(task);
            iterator.remove();
        }

        logger.info("Running tasks: " + runningTasks.stream().map(task -> task.id).collect(Collectors.toList()));
        return runningTasks;
    }

    private void initializeQueue(List<Task> tasks, Queue<Task> queue) {
        for (Task task : tasks) {
            task.priorityWeight = priorityToWeight.get(task.nice + 20);
            task.originalReadTime = task.readTime;
            task.originalBodyTime = task.bodyTime;
            task.originalWriteTime = task.writeTime;
            task.currentPeriodStart = task.startTime;
            queue.add(task.copy());
        }
    }

    private Queue<Task> copyQueue(Queue<Task> originalQueue) {
        Queue<Task> newQueue = new PriorityQueue<>(Comparator.comparingDouble(task -> task.priorityWeight));
        for (Task task : originalQueue) {
            newQueue.add(task.copy());
        }
        return newQueue;
    }


    private void addPeriodicJobs(List<Task> tasks, Queue<Task> queue, int time) {
        for (Task task : tasks) {
            if (time > task.startTime && task.period > 0 && time % task.period == 0) {
                task.currentPeriodStart = time;
                queue.add(task.copy());
                logger.info("Task " + task.id + " released with read time " + task.readTime + ", write time " + task.writeTime + ", body Time " + task.bodyTime);
            }
        }
    }

    private void skipReadStageIfNoReadTime(Task task) {
        if (task.stage == Stage.READ && task.readTime <= 0) {
            task.stage = Stage.BODY;
        }
    }


    private void displayResult(List<Double> WCRT, Queue<Task> queue) {
        logger.info("\n******************************");
        logger.info("***** Simulation Results *****");
        logger.info("******************************");
        logger.info("Unfinished tasks: " + queue.stream().map(task -> task.id).collect(Collectors.toList()));
        for (int i = 0; i < WCRT.size(); i++) {
            logger.info("Task " + (i+1) + " WCRT: " + WCRT.get(i));
        }
    }

    private boolean noReadAndWriteTasksRunning(List<Task> runningTasks, BlockingPolicy blockingPolicy) {
        return runningTasks.stream().noneMatch(task -> task.stage == Stage.READ || task.stage == Stage.WRITE) || blockingPolicy == BlockingPolicy.NONE;
    }

    private void initializelogger() {
        try {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
            String formatDateTime = now.format(formatter);

            FileHandler fileHandler = new FileHandler("./logs/simulation_" + formatDateTime + ".txt");
            fileHandler.setFormatter(new CustomFormatter());
            logger.setUseParentHandlers(false);
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addConsoleLogger() {
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new CustomFormatter());
        logger.addHandler(consoleHandler);
    }

    private boolean withinTolerance(double a, double b) {
        double tolerance = 1E-10;
        return (a - b) < tolerance;
    }

    private int getLCM(List<Task> tasks) {
        return tasks.stream().map(task -> task.period)
                .reduce(1, (a, b) -> a * (b / getGCD(a, b)));
    }

    private int getGCD(int a, int b) {
        if (b == 0) {
            return a;
        } else {
            return getGCD(b, a % b);
        }
    }
}
