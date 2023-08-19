package org.cap.model;

public class CoreState {
    public Task currentTask;
    public boolean isRunning;
    public int remainingRuntime;

    public CoreState() {
        this.currentTask = null;
        this.isRunning = false;
        this.remainingRuntime = 0;
    }
}
