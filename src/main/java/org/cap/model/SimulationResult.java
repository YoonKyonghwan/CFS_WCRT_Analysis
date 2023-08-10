package org.cap.model;

import java.util.List;

public class SimulationResult {
    public boolean schedulability;
    public List<List<Double>> WCRTs;

    public SimulationResult(boolean schedulability, List<List<Double>> WCRTs) {
        this.schedulability = schedulability;
        this.WCRTs = WCRTs;
    }
}
