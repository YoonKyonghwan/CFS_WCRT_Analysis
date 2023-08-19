package org.cap.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CFSSimulationState {

    public int targetedLatency = 20;
    public int minimumGranularity = 4;
    public BlockingPolicy blockingPolicy = BlockingPolicy.NONE;
    public List<CoreState> coreStates;
    // TODO consider moving WCRTs and Queues to here after completion

    public CFSSimulationState(int targetedLatency, int minimumGranularity, int numberOfCores) {
        this.targetedLatency = targetedLatency;
        this.minimumGranularity = minimumGranularity;

        this.coreStates = new ArrayList<>(Collections.nCopies(numberOfCores, new CoreState()));
    }
}

