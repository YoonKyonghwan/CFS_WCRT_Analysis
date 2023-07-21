package org.cap;

public class Task {
    int id;
    int startTime;
    double readTime;
    double bodyTime;
    double writeTime;
    int nice;
    int period;
    // TODO add description for below fields
    Stage stage = Stage.READ;
    double originalReadTime;
    double originalBodyTime;
    double originalWriteTime;
    int currentPeriodStart;
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
}