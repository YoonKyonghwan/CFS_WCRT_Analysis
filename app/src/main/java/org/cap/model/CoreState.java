package org.cap.model;

import java.util.HashMap;
import java.util.Map.Entry;

public class CoreState {
    public Task currentTask;
    public boolean isRunning;
    public long remainingRuntime;
    public long minimumVirtualRuntime = 0L;
    HashMap<Integer, Long> lastVirtualRuntime;

    public void putFinishedTaskVirtualRuntime(Integer taskID, long virtualRuntime) {
        this.lastVirtualRuntime.put(taskID, virtualRuntime);
    }

    public long getLastVirtualRuntime(Integer taskID) {
        if(this.lastVirtualRuntime.containsKey(taskID)) {
            return this.lastVirtualRuntime.get(taskID);
        } else {
            return 0L;
        }
    }

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
        newState.lastVirtualRuntime = new HashMap<Integer, Long>();
        for (Entry<Integer, Long> entry : this.lastVirtualRuntime.entrySet()) {
            newState.lastVirtualRuntime.put(Integer.valueOf(entry.getKey().intValue()), Long.valueOf(entry.getValue().longValue()));
        }
        
        return newState;
    }

    public CoreState() {
        this.currentTask = null;
        this.isRunning = false;
        this.remainingRuntime = 0;
        this.lastVirtualRuntime = new HashMap<Integer, Long>();
    }
}
