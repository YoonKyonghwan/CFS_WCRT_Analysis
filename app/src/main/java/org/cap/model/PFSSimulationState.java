package org.cap.model;

public class PFSSimulationState {
    public BlockingPolicy blockingPolicy;
    public String writingTaskKey;

    public PFSSimulationState(BlockingPolicy blockingPolicy, String writingTaskKey) {
        this.blockingPolicy = blockingPolicy;
        this.writingTaskKey = writingTaskKey;
    }

}