package org.cap;

public class SimulationState {
    BlockingPolicy blockingPolicy;
    String writingTaskKey;

    public SimulationState copy() {
        SimulationState newSimulationState = new SimulationState();
        newSimulationState.blockingPolicy = this.blockingPolicy;
        newSimulationState.writingTaskKey = this.writingTaskKey;
        return newSimulationState;
    }

    public SimulationState(BlockingPolicy blockingPolicy, String writingTaskKey) {
        this.blockingPolicy = blockingPolicy;
        this.writingTaskKey = writingTaskKey;
    }

    public SimulationState() {
    }
}