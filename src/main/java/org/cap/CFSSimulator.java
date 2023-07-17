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
            // TODO need to select running tasks that match blocking policy
            // TODO check blockingPolicy 
            // TODO case 1: there are no previous tasks running
            // TODO case 2: there are previous tasks running
            List<Task> runningTasks = initializeRunningTasks(queue, tasks, time);

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
                // TODO subtract allocation appropriate for each read, body, write stage
                // currentTask.WCET -= allocation;

                // TODO set currentStage differently if the stage has ended
                // Re-queue the task if it is not finished
                if (currentTask.readTime > 0 || currentTask.bodyTime > 0 || currentTask.writeTime > 0) {
                    // TODO save blocking state if task at read or write stage is not finished
                    // TODO set currentStage as one of read, body, write
                    queue.add(currentTask);
                } else {
                    // TODO save RT of all jobs at the end
                    System.out.println("Task " + currentTask.id + " completed at time " + (time + 1) + " with RT " + (time - currentTask.currentPeriodStart + 1));
                    WCRT.set(currentTask.id - 1, Math.max(WCRT.get(currentTask.id - 1), time - currentTask.currentPeriodStart + 1));
                }
            }

            time += 1;
        }

        displayResult(WCRT, queue);
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

    private List<Task> initializeRunningTasks(Queue<Task> queue, List<Task> tasks, int time) {
        List<Task> runningTasks = new ArrayList<>();
        Iterator<Task> iterator = queue.iterator();

        // Add tasks to the queue if their start time has come
        while (iterator.hasNext()) {
            Task task = iterator.next();
            // TODO use blockingPolicy to select tasks accordingly
            // TODO use case and default
            // TODO if blockingPolicy == read, select tasks in read, body stage
            // TODO if blockingPolicy == write, select tasks in body, write stage
            // TODO if blockingPolicy == body, select tasks in read, body, write stage
            // TODO if both read, write stage tasks exist, select only one of them and diverge the path
            // TODO if read stage is running, then select tasks that are in read, body stage
            // TODO if body stage is running, then select tasks that are in body stage
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
