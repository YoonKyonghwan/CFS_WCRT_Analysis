package org.cap;

import java.util.*;
import java.util.stream.Collectors;

public class CFSSimulator {
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

    // TODO Make test cases
    // TODO Refactor and improve readability
    public ArrayList<Double> simulateCFS(List<Task> tasks) {
        System.out.println("Starting CFS simulation");
        ArrayList<Double> WCRT = new ArrayList<>(Collections.nCopies(tasks.size(), 0.0));
        SimulationState simulationState = new SimulationState(BlockingPolicy.NONE, "-1:0");
        Queue<Task> queue = new PriorityQueue<>(Comparator.comparingDouble(task -> task.priorityWeight));
        initializeQueue(tasks, queue);

        int time = 0;
        performSimulation(tasks, WCRT, simulationState, time, queue);

        displayResult(WCRT, queue);
        return WCRT;
    }

    private void performSimulation(List<Task> tasks, ArrayList<Double> WCRT, SimulationState simulationState, int time, Queue<Task> queue) {
        while (time < getLCM(tasks)) {
            System.out.printf("\n>>> CURRENT TIME: %d <<<\n", time);
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
            System.out.println("Blocking policy " + simulationState.blockingPolicy);
        }
    }

    private ArrayList<Double> simulatePath(List<Task> tasks, Queue<Task> queue, ArrayList<Double> WCRT, int time, SimulationState simulationState) {
        System.out.println("\n******* Path diverged *******");

        Queue<Task> cloneQueue = copyQueue(queue);
        ArrayList<Double> cloneWCRT = new ArrayList<>(WCRT);

        performSimulation(tasks, cloneWCRT, simulationState, time, cloneQueue);

        displayResult(cloneWCRT, cloneQueue);
        return cloneWCRT;
    }

    private static void executeTask(Task currentTask, double allocation, Queue<Task> queue, ArrayList<Double> WCRT, SimulationState simulationState, int time) {
        skipReadStageIfNoReadTime(currentTask);
        System.out.println("Task " + currentTask.id + " executed for " + allocation + " | stage: " + currentTask.stage);
        switch (currentTask.stage) {
            case READ:
                currentTask.readTime -= allocation;
                if (currentTask.readTime <= 0) {
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
                if (currentTask.bodyTime <= 0) {
                    if (currentTask.writeTime > 0)
                        currentTask.stage = Stage.WRITE;
                    else
                        currentTask.stage = Stage.COMPLETED;
                }
                break;
            case WRITE:
                currentTask.writeTime -= allocation;
                if (currentTask.writeTime <= 0) {
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
            System.out.println("Task " + currentTask.id + " completed at time " + (time + 1) + " with RT " + (time - currentTask.currentPeriodStart + 1));
            WCRT.set(currentTask.id - 1, Math.max(WCRT.get(currentTask.id - 1), time - currentTask.currentPeriodStart + 1));
        }
    }

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
                System.out.println("Task " + task.id + " released with read time " + task.readTime + ", write time " + task.writeTime + ", body Time " + task.bodyTime);
            }
        }
    }

    /**
     * Add tasks to the queue if their start time has come
     * Case 1: if blockingPolicy == READ, select tasks in read, body stage
     * Case 2: if blockingPolicy == WRITE, select tasks in body, write stage
     *         need to select previously running writing task
     * Case 3: if blockingPolicy == NONE, select tasks in read, body, write stage
     *         if read task is selected, set blockingPolicy to READ
     *         if write task is selected, set blockingPolicy to WRITE and set writingTaskKey
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

        System.out.println("Running tasks: " + runningTasks.stream().map(task -> task.id).collect(Collectors.toList()));
        return runningTasks;
    }

    private static void skipReadStageIfNoReadTime(Task task) {
        if (task.stage == Stage.READ && task.readTime <= 0) {
            task.stage = Stage.BODY;
        }
    }


    private void displayResult(List<Double> WCRT, Queue<Task> queue) {
        // TODO save simulation result as a file
        // TODO remove running logs when print (only include in file)
        System.out.println("\n******************************");
        System.out.println("***** Simulation Results *****");
        System.out.println("******************************");
        System.out.println("Unfinished tasks: " + queue.stream().map(task -> task.id).collect(Collectors.toList()));
        for (int i = 0; i < WCRT.size(); i++) {
            System.out.println("Task " + (i+1) + " WCRT: " + WCRT.get(i));
        }
    }

    private static boolean noReadAndWriteTasksRunning(List<Task> runningTasks, BlockingPolicy blockingPolicy) {
        return runningTasks.stream().noneMatch(task -> task.stage == Stage.READ || task.stage == Stage.WRITE) || blockingPolicy == BlockingPolicy.NONE;
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
