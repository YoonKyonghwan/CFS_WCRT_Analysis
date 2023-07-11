package org.cap;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

public class Main {

    public static void main(String[] args) {
        TaskReader taskReader = new TaskReader();
        List<Task> tasks = taskReader.readTasksFromFile("tasks.json");
        simulateCFS(tasks);
    }

    public static void simulateCFS(List<Task> tasks) {
        // Initialize the priority queue with the initial tasks
        Queue<Task> queue = new PriorityQueue<>((task1, task2) -> Double.compare(task1.priorityWeight, task2.priorityWeight));
        loadQueue(tasks, queue);

        double[] WCRT = new double[tasks.size()];
        int time = 0;

        while (time < getLCM(tasks) || !queue.isEmpty()) {
            // If there are no tasks in the queue, just increment the time
            if (queue.isEmpty()) {
                time++;
                continue;
            }

            List<Task> runningTasks = loadRunningTasks(tasks, time);

            // Calculate total priority weight
            double totalPriorityWeight = runningTasks.stream().mapToDouble(t -> t.priorityWeight).sum();

            // Share the CPU among all tasks proportionally to their priority weight
            for (Task currentTask : runningTasks) {
                double allocation = 1.0 * (currentTask.priorityWeight / totalPriorityWeight); // One second divided proportionally to priority weight
                currentTask.WCET -= allocation;

                if (currentTask.WCET > 0) {
                    queue.add(currentTask);  // Re-queue the task if it is not finished
                } else {
                    WCRT[currentTask.id-1] = Math.max(WCRT[currentTask.id-1], time + allocation);  // Update WCRT if the task has finished
                    currentTask.WCET = currentTask.originalWCET;
                }

                // Check if the period has come again
                if (currentTask.period > 0 && time % currentTask.period == 0) {
                    queue.add(currentTask);  // Re-queue the task if its period has come again
                }
            }

            time += 1;  // Increase time by one second
        }

        for (int i = 0; i < WCRT.length; i++) {
            System.out.println("Task " + (i+1) + " WCRT: " + WCRT[i]);
        }
    }

    private static List<Task> loadRunningTasks(List<Task> tasks, int time) {
        List<Task> runningTasks = new ArrayList<>();

        // Add tasks to the queue if their start time has come
        for (Task task : tasks) {
            if (task.startTime == time) {
                // remove from queue
                runningTasks.add(task);
            }
        }
        return runningTasks;
    }

    private static void loadQueue(List<Task> tasks, Queue<Task> queue) {
        for (Task task : tasks) {
            task.priorityWeight = Math.pow(1.25, task.nice + 20);
            task.originalWCET = task.WCET;
            if (task.startTime == 0) {
                queue.add(task);
            }
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