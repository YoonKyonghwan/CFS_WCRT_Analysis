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

    private static BlockingPolicy blockingPolicy = BlockingPolicy.NONE;

    private static int writingTaskId = -1;

    // 1. apply read, body, write blocking policy
    // TODO 2. diverge paths to get worst case
    // TODO 3. make test cases
    public void simulateCFS(List<Task> tasks) {
        System.out.println("Starting CFS simulation");
        ArrayList<Double> WCRT = new ArrayList<>(Collections.nCopies(tasks.size(), 0.0));
        int time = 0;

        // Initialize the priority queue with the initial tasks
        Queue<Task> queue = new PriorityQueue<>(Comparator.comparingDouble(task -> task.priorityWeight));
        initializeQueue(tasks, queue, time);

        while (time < getLCM(tasks)) {
            System.out.printf("\n>>> CURRENT TIME: %d <<<\n", time);

            // Check if the period has come again and re-queue tasks if necessary
            // TODO add release jobs logs
            addPeriodicJobs(tasks, queue, time);
            List<Task> runningTasks = initializeRunningTasks(queue, time);

            System.out.println("Running tasks: " + runningTasks.stream().map(task -> task.id).collect(Collectors.toList()));

            // If there are no tasks in runningTasks, just increment the time
            if (runningTasks.isEmpty()) {
                time++;
                continue;
            }

            // Calculate total priority weight
            double totalPriorityWeight = runningTasks.stream().mapToDouble(t -> t.priorityWeight).sum();

            // Share the CPU among all tasks proportionally to their priority weight
            for (Task currentTask : runningTasks) {
                double allocation = 1.0 * (currentTask.priorityWeight / totalPriorityWeight);
                System.out.println("Task " + currentTask.id + " executed for " + allocation);

                // Re-queue the task if it is not finished
                switch (currentTask.stage) {
                    case READ:
                        currentTask.readTime -= allocation;
                        if (currentTask.readTime <= 0) {
                            currentTask.stage = Stage.BODY;
                        }
                        else {
                            if (blockingPolicy == BlockingPolicy.NONE) {
                                blockingPolicy = BlockingPolicy.READ;
                            }
                        }
                        break;
                    case BODY:
                        currentTask.bodyTime -= allocation;
                        if (currentTask.bodyTime <= 0) {
                            currentTask.stage = Stage.WRITE;
                        }
                        break;
                    case WRITE:
                        currentTask.writeTime -= allocation;
                        if (currentTask.writeTime <= 0) {
                            currentTask.stage = Stage.COMPLETED;
                            writingTaskId = -1;
                        }
                        else {
                            if (blockingPolicy == BlockingPolicy.NONE) {
                                blockingPolicy = BlockingPolicy.WRITE;
                                writingTaskId = currentTask.id;
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

            // If no read, write tasks are running, reset blockingPolicy to NONE
            // TODO tasks' stages are already updated to new stage
            for (Task task : runningTasks) {
                System.out.println("Task " + task.id + " - " + task.stage);
            }
            System.out.println(runningTasks.stream().noneMatch(task -> task.stage == Stage.READ || task.stage == Stage.WRITE));
            // I think this is supposed to check if read and write tasks ended...
            // How to check if blocking policy became NONE or blocking policy was just NONE and we need to diverge?
            if (runningTasks.stream().noneMatch(task -> task.stage == Stage.READ || task.stage == Stage.WRITE) || blockingPolicy == BlockingPolicy.NONE) {
                blockingPolicy = BlockingPolicy.NONE;
                // TODO cases of diverging: when blocking policy is None, 1) read and write tasks exist, 2) multiple write tasks exist
                // TODO even in case of 1) multiple write tasks can exist
                // TODO need to simulate multiple write paths if they exist and get WCRT for each path
                // TODO need to not just specify which blocking policy but also writingTaskId to properly check all paths
                // TODO add a function simulatePath which can specify blocking policy and writingTaskId
                // TODO then combine all WCRT results from all paths
                // TODO what if path diverges within a path?
                // TODO maybe return WCRT for simulatePath function? then compare WCRTs from all paths and get max?
                // check if start time is greater than or equal to time + 1 value and see if read and write tasks both exist
                int nextTime = time + 1;
                List<Task> readTasks = tasks.stream().filter(task -> task.stage == Stage.READ && task.startTime >= nextTime).collect(Collectors.toList());
                List<Task> writeTasks = tasks.stream().filter(task -> task.stage == Stage.WRITE && task.startTime >= nextTime).collect(Collectors.toList());

                ArrayList<ArrayList<Double>> possibleWCRT = new ArrayList<ArrayList<Double>>();

                // Case 1: read and write tasks exist
                if (!readTasks.isEmpty() && !writeTasks.isEmpty()) {
                    possibleWCRT.add(simulatePath(tasks, queue, WCRT, time, writingTaskId, BlockingPolicy.READ));
                    for (Task writeTask : writeTasks) {
                        possibleWCRT.add(simulatePath(tasks, queue, WCRT, time, writeTask.id, BlockingPolicy.WRITE));
                    }

                    for (int i=0; i<WCRT.size(); i++) {
                        double maxWCRT = 0;
                        for (int j=0; j<possibleWCRT.size(); j++) {
                            maxWCRT = Math.max(maxWCRT, possibleWCRT.get(j).get(i));
                        }
                        WCRT.set(i, maxWCRT);
                    }
                    break;
                }
                // Case 2: multiple write tasks exist
                else if (writeTasks.size() > 1) {
                    for (Task writeTask : writeTasks) {
                        possibleWCRT.add(simulatePath(tasks, queue, WCRT, time, writeTask.id, BlockingPolicy.WRITE));
                    }

                    for (int i=0; i<WCRT.size(); i++) {
                        double maxWCRT = 0;
                        for (int j=0; j<possibleWCRT.size(); j++) {
                            maxWCRT = Math.max(maxWCRT, possibleWCRT.get(j).get(i));
                        }
                        WCRT.set(i, maxWCRT);
                    }
                    break;
                }
            }

            System.out.println("Blocking policy " + blockingPolicy);

            time += 1;
        }

        displayResult(WCRT, queue);
    }

    private ArrayList<Double> simulatePath(List<Task> tasks, Queue<Task> queue, ArrayList<Double> WCRT, int time, int writingTaskId, BlockingPolicy blockingPolicy) {
        System.out.println("Path diverged");
        // Simulate one path (either read or write)
        // This is basically the body of the original simulateCFS function, but with some modifications to handle a single path
        // ...
        while (time < getLCM(tasks)) {
            System.out.printf("\n>>> CURRENT TIME: %d <<<\n", time);

            addPeriodicJobs(tasks, queue, time);
            // TODO specify blockingPolicy and writingTaskId when initializing runningTasks
            // TODO maybe the function should receive blockingPolicy and writingTaskId optionally
            List<Task> runningTasks = initializeRunningTasks(queue, blockingPolicy, writingTaskId, time);

            System.out.println("Running tasks: " + runningTasks.stream().map(task -> task.id).collect(Collectors.toList()));

            // If there are no tasks in runningTasks, just increment the time
            if (runningTasks.isEmpty()) {
                time++;
                continue;
            }

            // Calculate total priority weight
            double totalPriorityWeight = runningTasks.stream().mapToDouble(t -> t.priorityWeight).sum();

            // Share the CPU among all tasks proportionally to their priority weight
            for (Task currentTask : runningTasks) {
                double allocation = 1.0 * (currentTask.priorityWeight / totalPriorityWeight);
                System.out.println("Task " + currentTask.id + " executed for " + allocation);

                // Re-queue the task if it is not finished
                switch (currentTask.stage) {
                    case READ:
                        currentTask.readTime -= allocation;
                        if (currentTask.readTime <= 0) {
                            currentTask.stage = Stage.BODY;
                        }
                        else {
                            if (blockingPolicy == BlockingPolicy.NONE) {
                                blockingPolicy = BlockingPolicy.READ;
                            }
                        }
                        break;
                    case BODY:
                        currentTask.bodyTime -= allocation;
                        if (currentTask.bodyTime <= 0) {
                            currentTask.stage = Stage.WRITE;
                        }
                        break;
                    case WRITE:
                        currentTask.writeTime -= allocation;
                        if (currentTask.writeTime <= 0) {
                            currentTask.stage = Stage.COMPLETED;
                            writingTaskId = -1;
                        }
                        else {
                            if (blockingPolicy == BlockingPolicy.NONE) {
                                blockingPolicy = BlockingPolicy.WRITE;
                                writingTaskId = currentTask.id;
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

            // If no read, write tasks are running, reset blockingPolicy to NONE
            if (runningTasks.stream().noneMatch(task -> task.stage == Stage.READ || task.stage == Stage.WRITE) || blockingPolicy == BlockingPolicy.NONE) {
                blockingPolicy = BlockingPolicy.NONE;
                // TODO cases of diverging: when blocking policy is None, 1) read and write tasks exist, 2) multiple write tasks exist
                // TODO even in case of 1) multiple write tasks can exist
                // TODO need to simulate multiple write paths if they exist and get WCRT for each path
                // TODO need to not just specify which blocking policy but also writingTaskId to properly check all paths
                // TODO add a function simulatePath which can specify blocking policy and writingTaskId
                // TODO then combine all WCRT results from all paths
                // TODO what if path diverges within a path?
                // TODO maybe return WCRT for simulatePath function? then compare WCRTs from all paths and get max?
                // check if start time is greater than or equal to time + 1 value and see if read and write tasks both exist
                int nextTime = time + 1;
                List<Task> readTasks = tasks.stream().filter(task -> task.stage == Stage.READ && task.startTime >= nextTime).collect(Collectors.toList());
                List<Task> writeTasks = tasks.stream().filter(task -> task.stage == Stage.WRITE && task.startTime >= nextTime).collect(Collectors.toList());

                // TODO make an array of WCRT for each path and compare them
                ArrayList<ArrayList<Double>> possibleWCRT = new ArrayList<ArrayList<Double>>();

                // TODO modify condition to account for second case of diverging

                // Case 1: read and write tasks exist
                if (!readTasks.isEmpty() && !writeTasks.isEmpty()) {
                    possibleWCRT.add(simulatePath(tasks, queue, WCRT, time, writingTaskId, BlockingPolicy.READ));
                    for (Task writeTask : writeTasks) {
                        possibleWCRT.add(simulatePath(tasks, queue, WCRT, time, writeTask.id, BlockingPolicy.WRITE));
                    }

                    // TODO compare WCRT and get max
                    // TODO within simulatePath, it would return final WCRT so that it can be compared
                    for (int i=0; i<WCRT.size(); i++) {
                        double maxWCRT = 0;
                        for (int j=0; j<possibleWCRT.size(); j++) {
                            maxWCRT = Math.max(maxWCRT, possibleWCRT.get(j).get(i));
                        }
                        WCRT.set(i, maxWCRT);
                    }
                    break;
                }
                // Case 2: multiple write tasks exist
                else if (writeTasks.size() > 1) {
                    for (Task writeTask : writeTasks) {
                        possibleWCRT.add(simulatePath(tasks, queue, WCRT, time, writeTask.id, BlockingPolicy.WRITE));
                    }

                    // TODO compare WCRT and get max
                    // TODO within simulatePath, it would return final WCRT so that it can be compared
                    for (int i=0; i<WCRT.size(); i++) {
                        double maxWCRT = 0;
                        for (int j=0; j<possibleWCRT.size(); j++) {
                            maxWCRT = Math.max(maxWCRT, possibleWCRT.get(j).get(i));
                        }
                        WCRT.set(i, maxWCRT);
                    }
                    break;
                }
            }

            System.out.println("Blocking policy " + blockingPolicy);

            time += 1;
        }
        return WCRT;
    }

    private void initializeQueue(List<Task> tasks, Queue<Task> queue, int time) {
        for (Task task : tasks) {
            task.priorityWeight = priorityToWeight.get(task.nice + 20);
            task.originalReadTime = task.readTime;
            task.originalBodyTime = task.bodyTime;
            task.originalWriteTime = task.writeTime;
            task.currentPeriodStart = time;
            queue.add(task);
        }
    }

    private void addPeriodicJobs(List<Task> tasks, Queue<Task> queue, int time) {
        for (Task task : tasks) {
            if (time > task.startTime && task.period > 0 && time % task.period == 0) {
                task.currentPeriodStart = time;
                queue.add(task);
                System.out.println("Task " + task.id + " released with read time " + task.readTime + ", write time " + task.writeTime + ", body Time " + task.bodyTime);
            }
        }
    }

    private List<Task> initializeRunningTasks(Queue<Task> queue, int time) {
        List<Task> runningTasks = new ArrayList<>();
        Iterator<Task> iterator = queue.iterator();

        // Add tasks to the queue if their start time has come
        // if blockingPolicy == READ, select tasks in read, body stage
        // if blockingPolicy == WRITE, select tasks in body, write stage
        // need to select previously running writing task
        // if blockingPolicy == NONE, select tasks in read, body, write stage
        // if both read, write stage tasks exist, select only one of them and diverge the path
        // if read stage is running, then select tasks that are in read, body stage
        while (iterator.hasNext()) {
            Task task = iterator.next();
            switch (blockingPolicy) {
                case NONE:
                    // TODO modification needed
                    if (task.stage == Stage.WRITE)
                        blockingPolicy = BlockingPolicy.WRITE;
                    else if (task.stage == Stage.READ)
                        blockingPolicy = BlockingPolicy.READ;
                    break;
                case READ:
                    if (task.stage == Stage.READ || task.stage == Stage.BODY)
                        break;
                    else
                        continue;
                case WRITE:
                    if (task.id == writingTaskId || task.stage == Stage.BODY)
                        break;
                    else
                        continue;
            }
            if (task.startTime <= time) {
                runningTasks.add(task);
                iterator.remove();
            }
        }

        return runningTasks;
    }

    private List<Task> initializeRunningTasks(Queue<Task> queue, BlockingPolicy blockingPolicy, int writingTaskId, int time) {
        List<Task> runningTasks = new ArrayList<>();
        Iterator<Task> iterator = queue.iterator();

        // Add tasks to the queue if their start time has come
        // if blockingPolicy == READ, select tasks in read, body stage
        // if blockingPolicy == WRITE, select tasks in body, write stage
        // need to select previously running writing task
        // if blockingPolicy == NONE, select tasks in read, body, write stage
        // if both read, write stage tasks exist, select only one of them and diverge the path
        // if read stage is running, then select tasks that are in read, body stage
        while (iterator.hasNext()) {
            Task task = iterator.next();
            switch (blockingPolicy) {
                case NONE:
                    // TODO modification needed
                    if (task.stage == Stage.WRITE)
                        blockingPolicy = BlockingPolicy.WRITE;
                    else if (task.stage == Stage.READ)
                        blockingPolicy = BlockingPolicy.READ;
                    break;
                case READ:
                    if (task.stage == Stage.READ || task.stage == Stage.BODY)
                        break;
                    else
                        continue;
                case WRITE:
                    if (task.id == writingTaskId || task.stage == Stage.BODY)
                        break;
                    else
                        continue;
            }
            if (task.startTime <= time) {
                runningTasks.add(task);
                iterator.remove();
            }
        }

        return runningTasks;
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
