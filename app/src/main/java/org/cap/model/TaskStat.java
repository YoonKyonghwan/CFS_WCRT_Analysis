package org.cap.model;

public class TaskStat {
    public Task task;
    // Current stage of the task
    public Stage stage = Stage.READ;

    // Release time for read stage of the task
    public long readReleaseTime;

    // Release time for body stage of the task
    public long bodyReleaseTime;

    // Release time for write stage of the task
    public long writeReleaseTime;

    public long readTimeInNanoSeconds;

    public long bodyTimeInNanoSeconds;

    public long writeTimeInNanoSeconds;

    // Virtual runtime
    public long virtualRuntime;

    private long queueInsertTime = 0;

    public TaskStat copy() {
        TaskStat newTask = new TaskStat();
        newTask.task = this.task;
        newTask.stage = this.stage;
        newTask.readReleaseTime = this.readReleaseTime;
        newTask.bodyReleaseTime = this.bodyReleaseTime;
        newTask.writeReleaseTime = this.writeReleaseTime;
        newTask.virtualRuntime = this.virtualRuntime;
        newTask.queueInsertTime = this.queueInsertTime;
        newTask.readTimeInNanoSeconds = this.readTimeInNanoSeconds;
        newTask.bodyTimeInNanoSeconds = this.bodyTimeInNanoSeconds;
        newTask.writeTimeInNanoSeconds = this.writeTimeInNanoSeconds;
        return newTask;
    }

    public TaskStat(Task task) {
        this.task = task;
        this.readTimeInNanoSeconds = this.task.originalReadTime;
        this.bodyTimeInNanoSeconds = this.task.originalBodyTime;
        this.writeTimeInNanoSeconds = this.task.originalWriteTime;
        this.virtualRuntime = 0L;
        this.queueInsertTime = -1;
    }

    public TaskStat() {
        this.queueInsertTime = -1;
    }

    public void setQueueInsertTime(long queueInsertTime) {
        this.queueInsertTime = queueInsertTime;
    }

    public long getQueueInsertTime() {
        return queueInsertTime;
    }

    public int getId() {
        return this.task.id;
    }
}
