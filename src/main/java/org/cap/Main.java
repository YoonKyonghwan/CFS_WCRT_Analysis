package org.cap;

import java.util.List;
import java.util.PriorityQueue;

public class Main {

    public static void main(String[] args) {
        TaskReader taskReader = new TaskReader();
        List<Task> tasks = taskReader.readTasksFromFile("tasks.json");
        simulateCFS(tasks);
    }

    public static void simulateCFS(List<Task> tasks) {
        TaskQueue queue = new TaskQueue();
        queue.addAll(tasks);
        int time = 0;

        while (!queue.isEmpty()) {
            Task task = queue.poll();
            task.virtualRuntime += task.worstCaseExecutionTime / (task.nice + 20);
            time += task.worstCaseExecutionTime;
            task.responseTimes.add(time - task.startTime);
            System.out.println("Task with nice " + task.nice + " finishes at " + time);

            if (time < task.period) {
                task.startTime += task.period;
                task.virtualRuntime = 0;
                tasks.add(task);
            }
        }

        TaskWriter taskWriter = new TaskWriter();
        taskWriter.writeResponseTimes(tasks);
    }
}
