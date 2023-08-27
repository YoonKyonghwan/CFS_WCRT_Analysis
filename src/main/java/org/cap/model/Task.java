package org.cap.model;

import com.google.gson.annotations.Expose;

public class Task {
    // Unique identifier for the task
    @Expose
    public int id;

    // Start time for the task
    @Expose
    public int startTime;

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
    public int period;

    @Expose
    public int index;

    // Current stage of the task
    public Stage stage = Stage.READ;

    // Original time required to read the task
    public double originalReadTime;

    // Original time required for the task body
    public double originalBodyTime;

    // Original time required to write the task
    public double originalWriteTime;

    // Release time for read stage of the task
    public int readReleaseTime;

    // Release time for body stage of the task
    public int bodyReleaseTime;

    // Release time for write stage of the task
    public int writeReleaseTime;

    // Weight of task
    public double weight;

    // Virtual runtime
    public double virtualRuntime;

    // Execution time with Concurrent Execution Interference
    public int Eir;
    public int Eiw;
    public int Eib;
    public int WCRT_by_proposed;

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
        newTask.readReleaseTime = this.readReleaseTime;
        newTask.bodyReleaseTime = this.bodyReleaseTime;
        newTask.writeReleaseTime = this.writeReleaseTime;
        newTask.weight = this.weight;
        newTask.Eir = this.Eir;
        newTask.Eiw = this.Eiw;
        newTask.Eib = this.Eib;
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