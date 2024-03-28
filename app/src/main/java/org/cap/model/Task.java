package org.cap.model;

import com.google.gson.annotations.Expose;

public class Task {
    // Unique identifier for the task
    @Expose
    public int id;

    // Start time for the task
    @Expose
    public long startTime;

    // Remaining time required to read the task
    @Expose
    public double readTime;

    // Remaining time required for the task body
    @Expose
    public double bodyTime;

    // Remaining time required to write the task
    @Expose
    public double writeTime;

    // Nice value for the task
    @Expose
    public int nice;

    // Period of the task
    @Expose
    public long period;

    // start priority at time 0
    @Expose
    public int initialPriority;

    @Expose
    public int index;

    // Original time required to read the task
    public long originalReadTime;

    // Original time required for the task body
    public long originalBodyTime;

    // Original time required to write the task
    public long originalWriteTime;

    // Weight of task
    public long weight;

    // target task id
    public boolean isTargetTask;

    // Execution time with Concurrent Execution Interference
    public long WCRT_by_proposed;
    public boolean isSchedulable_by_proposed;
    public long WCRT_by_simulator;
    public boolean isSchedulable_by_simulator;
    public boolean isSchedulable_by_FIFO;
    public boolean isSchedulable_by_RR;
    public boolean isSchedulable_by_RM;

    private boolean initialized = false;

    private long queueInsertTime = 0;

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
        newTask.originalReadTime = this.originalReadTime;
        newTask.originalBodyTime = this.originalBodyTime;
        newTask.originalWriteTime = this.originalWriteTime;
        newTask.weight = this.weight;
        newTask.isTargetTask = this.isTargetTask;
        newTask.initialPriority = this.initialPriority;
        newTask.queueInsertTime = this.queueInsertTime;
        return newTask;
    }

    public void initializeMemberVariables() {
        if(this.initialized == false) {
            this.period = period * 1000L;
            this.originalReadTime = ((long) this.readTime) * 1000L;
            this.originalBodyTime = ((long) this.bodyTime) * 1000L;
            this.originalWriteTime = ((long) this.writeTime) * 1000L;
            this.weight = NiceToWeight.getWeight(this.nice);
            this.initialized = true;
        }
    }

    public Task(int id, int startTime, double readTime, double bodyTime, double writeTime, int nice, int period, int index) {
        this.id = id;
        this.startTime = startTime;
        this.readTime = readTime;
        this.bodyTime = bodyTime;
        this.writeTime = writeTime;
        this.nice = nice;
        this.period = period * 1000L;
        this.index = index;
        this.initialPriority = -1;
        this.initialized = true;
        this.queueInsertTime = -1;
    }

    public Task() {
        this.isTargetTask = false;
        this.initialPriority = -1;
        this.queueInsertTime = -1;
    }

    public void setQueueInsertTime(long queueInsertTime) {
        this.queueInsertTime = queueInsertTime;
    }

    public long getQueueInsertTime() {
        return queueInsertTime;
    }

    public int getId() {
        return id;
    }
}