package org.cap;

import java.util.PriorityQueue;

public class TaskQueue extends PriorityQueue<Task> {

    public TaskQueue() {
        super((task1, task2) -> Float.compare(task1.virtualRuntime, task2.virtualRuntime));
    }
}