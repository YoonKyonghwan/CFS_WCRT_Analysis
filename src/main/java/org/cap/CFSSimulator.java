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

    private static String writingTaskKey = "-1:0";

    // TODO Make test cases
    // TODO Refactor and improve readability
    public ArrayList<Double> simulateCFS(List<Task> tasks) {
        System.out.println("Starting CFS simulation");
        ArrayList<Double> WCRT = new ArrayList<>(Collections.nCopies(tasks.size(), 0.0));
        int time = 0;

        // Initialize the priority queue with the initial tasks
        Queue<Task> queue = new PriorityQueue<>(Comparator.comparingDouble(task -> task.priorityWeight));
        initializeQueue(tasks, queue);

        while (time < getLCM(tasks)) {
            System.out.printf("\n>>> CURRENT TIME: %d <<<\n", time);

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
                executeTask(currentTask, allocation, queue, WCRT, blockingPolicy, writingTaskKey, time);
            }

            time += 1;

            addPeriodicJobs(tasks, queue, time);

            // If no read, write tasks are running, reset blockingPolicy to NONE
            if (runningTasks.stream().noneMatch(task -> task.stage == Stage.READ || task.stage == Stage.WRITE) || blockingPolicy == BlockingPolicy.NONE) {
                blockingPolicy = BlockingPolicy.NONE;

                if (pathDiverges(tasks, queue, WCRT, writingTaskKey, time)) break;
            }
            System.out.println("Blocking policy " + blockingPolicy);
        }

        displayResult(WCRT, queue);
        return WCRT;
    }

    private static void executeTask(Task currentTask, double allocation, Queue<Task> queue, ArrayList<Double> WCRT, BlockingPolicy blockingPolicy, String writingTaskKey, int time) {
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
                    writingTaskKey = "-1:0";
                }
                else {
                    if (blockingPolicy == BlockingPolicy.NONE) {
                        blockingPolicy = BlockingPolicy.WRITE;
                        writingTaskKey = String.format("%s:%s", currentTask.id, currentTask.currentPeriodStart);
                    }
                }
                break;
        }
        System.out.println("Task " + currentTask.id + " executed for " + allocation + " | stage: " + currentTask.stage);

        if (currentTask.stage != Stage.COMPLETED) {
            queue.add(currentTask);
        } else {
            // TODO save RT of all jobs at the end
            System.out.println("Task " + currentTask.id + " completed at time " + (time + 1) + " with RT " + (time - currentTask.currentPeriodStart + 1));
            WCRT.set(currentTask.id - 1, Math.max(WCRT.get(currentTask.id - 1), time - currentTask.currentPeriodStart + 1));
        }
    }

    private ArrayList<Double> simulatePath(List<Task> tasks, Queue<Task> queue, ArrayList<Double> WCRT, int time, String writingTaskKey, BlockingPolicy blockingPolicy) {
        System.out.println("\n******************************");
        System.out.println("Path diverged");
        System.out.println("******************************");

        Queue<Task> cloneQueue = deepCopyQueue(queue);
        ArrayList<Double> cloneWCRT = new ArrayList<Double>(WCRT);
        System.out.println("Queue state: " + queue);
        System.out.println("Clone queue state: " + cloneQueue);

        // Simulate one path (either read or write)
        // This is basically the body of the original simulateCFS function, but with some modifications to handle a single path
        // ...
        while (time < getLCM(tasks)) {
            System.out.printf("\n>>> CURRENT TIME: %d <<<\n", time);

            List<Task> runningTasks = initializeRunningTasksForPath(cloneQueue, blockingPolicy, writingTaskKey, time);

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
                executeTask(currentTask, allocation, cloneQueue, cloneWCRT, blockingPolicy, writingTaskKey, time);
            }

            time += 1;

            addPeriodicJobs(tasks, cloneQueue, time);

            // If no read, write tasks are running, reset blockingPolicy to NONE
            if (runningTasks.stream().noneMatch(task -> task.stage == Stage.READ || task.stage == Stage.WRITE) || blockingPolicy == BlockingPolicy.NONE) {
                blockingPolicy = BlockingPolicy.NONE;

                if (pathDiverges(tasks, cloneQueue, cloneWCRT, writingTaskKey, time)) break;
            }

            System.out.println("Blocking policy: " + blockingPolicy);
        }

        displayResult(cloneWCRT, cloneQueue);
        return cloneWCRT;
    }

    private boolean pathDiverges(List<Task> tasks, Queue<Task> queue, ArrayList<Double> WCRT, String writingTaskKey, int time) {
        List<Task> readTasks = queue.stream().filter(task -> task.stage == Stage.READ && task.startTime <= time).collect(Collectors.toList());
        List<Task> writeTasks = queue.stream().filter(task -> task.stage == Stage.WRITE && task.startTime <= time).collect(Collectors.toList());

        ArrayList<ArrayList<Double>> possibleWCRT = new ArrayList<>();

        // Case 1: read and write tasks exist
        if (!readTasks.isEmpty() && !writeTasks.isEmpty()) {
            possibleWCRT.add(simulatePath(tasks, queue, WCRT, time, writingTaskKey, BlockingPolicy.READ));
            for (Task writeTask : writeTasks) {
                possibleWCRT.add(simulatePath(tasks, queue, WCRT, time, String.format("%s:%s", writeTask.id, writeTask.currentPeriodStart), BlockingPolicy.WRITE));
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
                possibleWCRT.add(simulatePath(tasks, queue, WCRT, time, String.format("%s:%s", writeTask.id, writeTask.currentPeriodStart), BlockingPolicy.WRITE));
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

    private Queue<Task> deepCopyQueue(Queue<Task> originalQueue) {
        Queue<Task> newQueue = new PriorityQueue<>(Comparator.comparingDouble(task -> task.priorityWeight));
        for (Task task : originalQueue) {
            newQueue.add(task.copy());
        }
        return newQueue;
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

    private void addPeriodicJobs(List<Task> tasks, Queue<Task> queue, int time) {
        for (Task task : tasks) {
            if (time > task.startTime && task.period > 0 && time % task.period == 0) {
                task.currentPeriodStart = time;
                queue.add(task.copy());
                System.out.println("Task " + task.id + " released with read time " + task.readTime + ", write time " + task.writeTime + ", body Time " + task.bodyTime);
            }
        }
    }

    private List<Task> initializeRunningTasks(Queue<Task> queue, int time) {
        List<Task> runningTasks = new ArrayList<>();
        Iterator<Task> iterator = queue.iterator();

        // Add tasks to the queue if their start time has come
        // Case 1: if blockingPolicy == READ, select tasks in read, body stage
        // Case 2: if blockingPolicy == WRITE, select tasks in body, write stage
        //         need to select previously running writing task
        // Case 3: if blockingPolicy == NONE, select tasks in read, body, write stage
        //         if both read, write stage tasks exist, select only one of them and diverge the path
        //         if read stage is running, then select tasks that are in read, body stage
        while (iterator.hasNext()) {
            Task task = iterator.next();
            if (task.startTime > time)
                continue;

            switch (blockingPolicy) {
                case NONE:
                    if (task.stage == Stage.READ)
                        blockingPolicy = BlockingPolicy.READ;
                    else if (task.stage == Stage.WRITE) {
                        blockingPolicy = BlockingPolicy.WRITE;
                        writingTaskKey = String.format("%s:%s", task.id, task.currentPeriodStart);
                    }
                    break;
                case READ:
                    if (task.stage == Stage.READ || task.stage == Stage.BODY)
                        break;
                    else
                        continue;
                case WRITE:
                    if (writingTaskKey.equals((String.format("%s:%s", task.id, task.currentPeriodStart))) || task.stage == Stage.BODY)
                        break;
                    else
                        continue;
            }
            runningTasks.add(task);
            iterator.remove();
        }

        return runningTasks;
    }

    // Function for simulatePath
    private List<Task> initializeRunningTasksForPath(Queue<Task> queue, BlockingPolicy blockingPolicy, String writingTaskKey, int time) {
        List<Task> runningTasks = new ArrayList<>();
        Iterator<Task> iterator = queue.iterator();

        // Add tasks to the queue if their start time has come
        // Case 1: if blockingPolicy == READ, select tasks in read, body stage
        // Case 2: if blockingPolicy == WRITE, select tasks in body, write stage
        //         need to select previously running writing task
        // Case 3: if blockingPolicy == NONE, select tasks in read, body, write stage
        //         if both read, write stage tasks exist, select only one of them and diverge the path
        //         if read stage is running, then select tasks that are in read, body stage
        while (iterator.hasNext()) {
            Task task = iterator.next();
            if (task.startTime > time)
                continue;

            switch (blockingPolicy) {
                case NONE:
                    if (task.stage == Stage.READ)
                        blockingPolicy = BlockingPolicy.READ;
                    else if (task.stage == Stage.WRITE) {
                        blockingPolicy = BlockingPolicy.WRITE;
                        writingTaskKey = String.format("%s:%s", task.id, task.startTime);
                    }
                    break;
                case READ:
                    if (task.stage == Stage.READ || task.stage == Stage.BODY)
                        break;
                    else
                        continue;
                case WRITE:
                    if (writingTaskKey.equals((String.format("%s:%s", task.id, task.currentPeriodStart))) || task.stage == Stage.BODY)
                        break;
                    else
                        continue;
            }
            runningTasks.add(task);
            iterator.remove();
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
