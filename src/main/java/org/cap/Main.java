package org.cap;

import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        TaskReader taskReader = new TaskReader();
        List<Task> tasks = taskReader.readTasksFromFile("tasks.json");
        simulateCFS(tasks);
    }

    public static void simulateCFS(List<Task> tasks) {
        List<Task> allTasks = new ArrayList<>(tasks);
        TaskQueue queue = new TaskQueue();
        int time = 0;

        while (time < getLCM(tasks)) {
            // Add tasks to queue if their start time has arrived
            int currentTime = time;
            tasks.removeIf(task -> {
                if (task.startTime <= currentTime) {
                    queue.add(task);
                    return true;
                }
                return false;
            });

            // If queue is empty, increment time and continue
            if (queue.isEmpty()) {
                time++;
                continue;
            }

            // Run task with lowest virtual runtime
            Task task = queue.poll();
            task.virtualRuntime += (float) task.worstCaseExecutionTime / (task.nice + 20);
            time += task.worstCaseExecutionTime;
            if (time - task.startTime > task.worstCaseResponseTime)
                task.worstCaseResponseTime = time - task.startTime;
            System.out.println(time + "s - id " + task.id + " task finished (vruntime = " + task.virtualRuntime + ")");

            // Add task back to queue if it has a period
            if (task.period != 0) {
                task.startTime += task.period;
                task.virtualRuntime = 0;
                tasks.add(task);
            }
        }

        TaskWriter taskWriter = new TaskWriter();
        taskWriter.writeResponseTimes(allTasks);
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