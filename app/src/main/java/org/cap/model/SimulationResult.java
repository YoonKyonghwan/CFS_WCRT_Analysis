package org.cap.model;

import java.util.HashMap;

public class SimulationResult {
    public boolean schedulability;
    public HashMap<Integer, Long> wcrtMap;

    public SimulationResult(boolean schedulability, HashMap<Integer, Long> wcrtMap) {
        this.schedulability = schedulability;
        this.wcrtMap = wcrtMap;
    }
}
