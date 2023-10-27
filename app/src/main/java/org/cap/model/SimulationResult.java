package org.cap.model;

import java.util.HashMap;

public class SimulationResult {
    public boolean schedulability;
    public HashMap<Integer, Double> wcrtMap;

    public SimulationResult(boolean schedulability, HashMap<Integer, Double> wcrtMap) {
        this.schedulability = schedulability;
        this.wcrtMap = wcrtMap;
    }
}
