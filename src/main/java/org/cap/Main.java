package org.cap;

import java.util.*;

public class Main {

    public static void main(String[] args) {
        TaskReader taskReader = new TaskReader();
        List<Task> tasks = taskReader.readTasksFromFile("tasks.json");
        simulateCFS(tasks);
    }

    public static void simulateCFS(List<Task> tasks) {
        // double[] WCRT = new double[tasks.size()];
        ArrayList<Double> WCRT = new ArrayList<>(Collections.nCopies(tasks.size(), 0.0));
        int time = 0;

        // Initialize the priority queue with the initial tasks
        Queue<Task> queue = new PriorityQueue<>(Comparator.comparingDouble(task -> task.priorityWeight));
        initializeQueue(tasks, queue, time);

        while (time < getLCM(tasks) || !queue.isEmpty()) {
            // Check if the period has come again and re-queue tasks if necessary
            addPeriodicJobs(tasks, queue, time);
            List<Task> runningTasks = initializeRunningTasks(queue, tasks, time);

            // If there are no tasks in runningTasks, just increment the time
            if (runningTasks.isEmpty()) {
                time++;
                continue;
            }

            // Calculate total priority weight
            double totalPriorityWeight = runningTasks.stream().mapToDouble(t -> t.priorityWeight).sum();

            // Share the CPU among all tasks proportionally to their priority weight
            for (Task currentTask : runningTasks) {
                double allocation = 1.0 * (currentTask.priorityWeight / totalPriorityWeight); // One second divided proportionally to priority weight
                currentTask.WCET -= allocation;

                if (currentTask.WCET > 0) {
                    queue.add(currentTask);  // Re-queue the task if it is not finished
                } else {
                    WCRT.set(currentTask.id - 1, Math.max(WCRT.get(currentTask.id - 1), time - currentTask.currentPeriodStart + 1));  // Update WCRT if the task has finished
                    currentTask.WCET = currentTask.originalWCET;
                }
            }

            time += 1;  // Increase time by one second
        }

        displayResult(WCRT);
    }

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

    private static void initializeQueue(List<Task> tasks, Queue<Task> queue, int time) {
        for (Task task : tasks) {
            task.priorityWeight = priorityToWeight.get(task.nice + 20);
            task.originalWCET = task.WCET;
            task.currentPeriodStart = time;
            queue.add(task);
        }
    }

    private static void addPeriodicJobs(List<Task> tasks, Queue<Task> queue, int time) {
        for (Task task : tasks) {
            if (time > task.startTime && task.period > 0 && time % task.period == 0) {
                task.currentPeriodStart = time;
                queue.add(task);
            }
        }
    }

    private static List<Task> initializeRunningTasks(Queue<Task> queue, List<Task> tasks, int time) {
        List<Task> runningTasks = new ArrayList<>();
        Iterator<Task> iterator = queue.iterator();

        // Add tasks to the queue if their start time has come
        while (iterator.hasNext()) {
            Task task = iterator.next();
            if (task.startTime <= time) {
                runningTasks.add(task);
                iterator.remove();
            }
        }

        return runningTasks;
    }

    private static void displayResult(List<Double> WCRT) {
        for (int i = 0; i < WCRT.size(); i++) {
            System.out.println("Task " + (i+1) + " WCRT: " + WCRT.get(i));
        }
    }

    private static int getLCM(List<Task> tasks) {
        return tasks.stream().map(Task::getPeriod)
                .reduce(1, (a, b) -> a * (b / getGCD(a, b)));
    }

    private static int getGCD(int a, int b) {
        if (b == 0) {
            return a;
        } else {
            return getGCD(b, a % b);
        }
    }
}