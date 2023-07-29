package org.cap.model;

public class Task {
    // Unique identifier for the task
    public int id;

    // Start time for the task
    public int startTime;

    // Remaining time required to read the task
    public double readTime;

    // Remaining time required for the task body
    public double bodyTime;

    // Remaining time required to write the task
    public double writeTime;

    // Nice value for the task
    public int nice;

    // Period of the task
    public int period;

    public int index;

    // Current stage of the task
    public Stage stage = Stage.READ;

    // Original time required to read the task
    public double originalReadTime;

    // Original time required for the task body
    public double originalBodyTime;

    // Original time required to write the task
    public double originalWriteTime;

    // Release time for the task
    public int releaseTime;

    // Weight for task priority
    public double priorityWeight;

    public Task copy() {
        Task newTask = new Task();
        newTask.id = this.id;
        newTask.startTime = this.startTime;
        newTask.readTime = this.readTime;
        newTask.bodyTime = this.bodyTime;
        newTask.writeTime = this.writeTime;
        newTask.nice = this.nice;
        newTask.period = this.period;
        newTask.index = this.index;
        newTask.stage = this.stage;
        newTask.originalReadTime = this.originalReadTime;
        newTask.originalBodyTime = this.originalBodyTime;
        newTask.originalWriteTime = this.originalWriteTime;
        newTask.releaseTime = this.releaseTime;
        newTask.priorityWeight = this.priorityWeight;
        return newTask;
    }

    public Task(int id, int startTime, double readTime, double bodyTime, double writeTime, int nice, int period, int index) {
        this.id = id;
        this.startTime = startTime;
        this.readTime = readTime;
        this.bodyTime = bodyTime;
        this.writeTime = writeTime;
        this.nice = nice;
        this.period = period;
        this.index = index;
    }

    public Task() {
    }
}