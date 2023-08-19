package org.cap.model;

public class CoreState {
    public Task currentTask;
    public boolean isRunning;
    public int remainingRuntime;

    public CoreState copy() {
        CoreState newState = new CoreState();
        newState.currentTask = this.currentTask;
        newState.isRunning = this.isRunning;
        newState.remainingRuntime = this.remainingRuntime;
        return newState;
    }

    public CoreState() {
        this.currentTask = null;
        this.isRunning = false;
        this.remainingRuntime = 0;
    }
}
