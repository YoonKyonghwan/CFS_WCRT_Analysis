package org.cap.model;

public class CoreState {
    public Task currentTask;
    public boolean isRunning;
    public int remainingRuntime;
    public long minimumVirtualRuntime = 0L;

    public CoreState copy() {
        CoreState newState = new CoreState();
        if( this.currentTask != null) {
            newState.currentTask = this.currentTask.copy();
        }
        else {
            newState.currentTask = null;
        }
        
        newState.isRunning = this.isRunning;
        newState.remainingRuntime = this.remainingRuntime;
        newState.minimumVirtualRuntime = this.minimumVirtualRuntime;
        return newState;
    }

    public CoreState() {
        this.currentTask = null;
        this.isRunning = false;
        this.remainingRuntime = 0;
    }
}
