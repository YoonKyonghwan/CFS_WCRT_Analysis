package org.cap;

public class SimulationState {
    BlockingPolicy blockingPolicy;
    String writingTaskKey;

    public SimulationState(BlockingPolicy blockingPolicy, String writingTaskKey) {
        this.blockingPolicy = blockingPolicy;
        this.writingTaskKey = writingTaskKey;
    }

}