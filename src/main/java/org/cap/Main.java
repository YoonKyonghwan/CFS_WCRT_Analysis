package org.cap;

import java.util.*;

public class Main {

    public static void main(String[] args) {
        TaskReader taskReader = new TaskReader();
        List<Task> tasks = taskReader.readTasksFromFile("tasks.json");
        simulateCFS(tasks);
    }

    public static void simulateCFS(List<Task> tasks) {
        // Initialize the priority queue with the initial tasks
        Queue<Task> queue = new PriorityQueue<>((task1, task2) -> Double.compare(task1.priorityWeight, task2.priorityWeight));

        double[] WCRT = new double[tasks.size()];
        int time = 0;
        loadQueue(tasks, queue, time);

        while (time < getLCM(tasks) || !queue.isEmpty()) {
            // Check if the period has come again and re-queue tasks if necessary
            reloadQueue(tasks, queue, time);

            List<Task> runningTasks = loadRunningTasks(queue, tasks, time);

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
                    WCRT[currentTask.id-1] = Math.max(WCRT[currentTask.id-1], time + allocation - currentTask.currentPeriodStart);  // Update WCRT if the task has finished
                    currentTask.WCET = currentTask.originalWCET;
                }
            }

            time += 1;  // Increase time by one second
        }

        displayResult(WCRT);
    }

    private static void reloadQueue(List<Task> tasks, Queue<Task> queue, int time) {
        for (Task task : tasks) {
            if (time > task.startTime && task.period > 0 && time % task.period == 0) {
                task.currentPeriodStart = time;
                queue.add(task);
            }
        }
    }

    private static void loadQueue(List<Task> tasks, Queue<Task> queue, int time) {
        for (Task task : tasks) {
            // TODO get real priority weight
            task.priorityWeight = Math.pow(1.25, task.nice + 20);
            task.originalWCET = task.WCET;
            task.currentPeriodStart = time;
            queue.add(task);
        }
    }

    private static List<Task> loadRunningTasks(Queue<Task> queue, List<Task> tasks, int time) {
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

    private static void displayResult(double[] WCRT) {
        for (int i = 0; i < WCRT.length; i++) {
            System.out.println("Task " + (i+1) + " WCRT: " + WCRT[i]);
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