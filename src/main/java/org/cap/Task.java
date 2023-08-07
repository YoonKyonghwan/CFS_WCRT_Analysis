package org.cap;

public class Task {
    // Unique identifier for the task
    int id;

    // Start time for the task
    int startTime;

    // Remaining time required to read the task
    double readTime;

    // Remaining time required for the task body
    double bodyTime;

    // Remaining time required to write the task
    double writeTime;

    // Nice value for the task
    int nice;

    // Period of the task
    int period;

    // Current stage of the task
    Stage stage = Stage.READ;

    // Original time required to read the task
    double originalReadTime;

    // Original time required for the task body
    double originalBodyTime;

    // Original time required to write the task
    double originalWriteTime;

    // Start of the current period
    int currentPeriodStart;

    // Weight for task priority
    double priorityWeight;

    public Task copy() {
        Task newTask = new Task();
        newTask.id = this.id;
        newTask.startTime = this.startTime;
        newTask.readTime = this.readTime;
        newTask.bodyTime = this.bodyTime;
        newTask.writeTime = this.writeTime;
        newTask.nice = this.nice;
        newTask.period = this.period;
        newTask.stage = this.stage;
        newTask.originalReadTime = this.originalReadTime;
        newTask.originalBodyTime = this.originalBodyTime;
        newTask.originalWriteTime = this.originalWriteTime;
        newTask.currentPeriodStart = this.currentPeriodStart;
        newTask.priorityWeight = this.priorityWeight;
        return newTask;
    }

    public Task(int id, int startTime, double readTime, double bodyTime, double writeTime, int nice, int period) {
        this.id = id;
        this.startTime = startTime;
        this.readTime = readTime;
        this.bodyTime = bodyTime;
        this.writeTime = writeTime;
        this.nice = nice;
        this.period = period;
    }

    public Task() {
    }
}