package org.cap.model;

import java.util.HashMap;

public class SimulationResult {
    public boolean schedulability;
    public HashMap<Integer, Long> wcrtMap;

    public SimulationResult(boolean schedulability, HashMap<Integer, Long> wcrtMap) {
        this.schedulability = schedulability;
        this.wcrtMap = wcrtMap;
    }

    public SimulationResult() {
        this.schedulability = true;
        this.wcrtMap = new HashMap<Integer, Long>();
    }

    public boolean isSchedulability() {
        return schedulability;
    }

    public void setSchedulability(boolean schedulability) {
        this.schedulability = schedulability;
    }

    public HashMap<Integer, Long> getWcrtMap() {
        return wcrtMap;
    }
}
