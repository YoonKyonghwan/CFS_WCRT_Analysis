package org.cap.model;

public class SimulationState {
    public BlockingPolicy blockingPolicy;
    public String writingTaskKey;

    public SimulationState(BlockingPolicy blockingPolicy, String writingTaskKey) {
        this.blockingPolicy = blockingPolicy;
        this.writingTaskKey = writingTaskKey;
    }

}