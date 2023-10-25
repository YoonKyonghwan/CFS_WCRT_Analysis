package org.cap.model;

import java.util.ArrayList;
import java.util.List;

public class CFSSimulationState {

    public int targetedLatency = 20;
    public int minimumGranularity = 4;
    public BlockingPolicy blockingPolicy = BlockingPolicy.NONE;
    public int blockingTaskId;
    public boolean blockingPolicyReset = false;
    public List<CoreState> coreStates;
    private ScheduleSimulationMethod method;

    // TODO consider moving WCRTs and Queues to here after completion

    public CFSSimulationState copy() {
        CFSSimulationState newState = new CFSSimulationState(this.method);
        newState.targetedLatency = this.targetedLatency;
        newState.minimumGranularity = this.minimumGranularity;
        newState.blockingPolicy = this.blockingPolicy;
        newState.blockingTaskId = this.blockingTaskId;
        newState.coreStates = new ArrayList<>(this.coreStates.size());
        for (CoreState coreState : this.coreStates) {
            newState.coreStates.add(coreState.copy());
        }
        return newState;
    }

    public CFSSimulationState(int targetedLatency, int minimumGranularity, int numberOfCores, ScheduleSimulationMethod method) {
        this.targetedLatency = targetedLatency;
        this.minimumGranularity = minimumGranularity;
        this.method = method;

        //this.coreStates = new ArrayList<>(Collections.nCopies(numberOfCores, new CoreState()));

        this.coreStates = new ArrayList<>();

        for(int i = 0 ; i < numberOfCores ; i++) {
            CoreState coreState = new CoreState();
            this.coreStates.add(coreState);
        }
    }

    public ScheduleSimulationMethod getMethod() {
        return method;
    }

    public void setMethod(ScheduleSimulationMethod method) {
        this.method = method;
    }

    public CFSSimulationState(ScheduleSimulationMethod method) {
        this.method = ScheduleSimulationMethod.BRUTE_FORCE;
    }
}

