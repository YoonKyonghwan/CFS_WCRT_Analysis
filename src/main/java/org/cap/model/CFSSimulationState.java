package org.cap.model;

public class CFSSimulationState {

    public int targetedLatency = 20;
    public int minimumGranularity = 4;
    public BlockingPolicy blockingPolicy = BlockingPolicy.NONE;
    public Task currentTask = null;
    public boolean isRunning = false;
    public int remainingRuntime = 0;
    public CFSSimulationState(int targetedLatency, int minimumGranularity, BlockingPolicy blockingPolicy, Task currentTask, boolean isRunning, int remainingRuntime) {
        this.targetedLatency = targetedLatency;
        this.minimumGranularity = minimumGranularity;
        this.blockingPolicy = blockingPolicy;
        this.currentTask = currentTask;
        this.isRunning = isRunning;
        this.remainingRuntime = remainingRuntime;
    }

    public CFSSimulationState(int targetedLatency, int minimumGranularity) {
        this.targetedLatency = targetedLatency;
        this.minimumGranularity = minimumGranularity;
    }
}