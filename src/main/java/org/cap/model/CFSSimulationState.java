package org.cap.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CFSSimulationState {

    public int targetedLatency = 20;
    public int minimumGranularity = 4;
    public BlockingPolicy blockingPolicy = BlockingPolicy.NONE;
    public int blockingTaskId;
    public List<CoreState> coreStates;
    // TODO consider moving WCRTs and Queues to here after completion

    public CFSSimulationState copy() {
        CFSSimulationState newState = new CFSSimulationState();
        newState.targetedLatency = this.targetedLatency;
        newState.minimumGranularity = this.minimumGranularity;
        newState.blockingPolicy = this.blockingPolicy;
        newState.coreStates = new ArrayList<>(this.coreStates.size());
        for (CoreState coreState : this.coreStates) {
            newState.coreStates.add(coreState.copy());
        }
        return newState;
    }

    public CFSSimulationState(int targetedLatency, int minimumGranularity, int numberOfCores) {
        this.targetedLatency = targetedLatency;
        this.minimumGranularity = minimumGranularity;

        this.coreStates = new ArrayList<>(Collections.nCopies(numberOfCores, new CoreState()));
    }

    public CFSSimulationState() {
    }
}

