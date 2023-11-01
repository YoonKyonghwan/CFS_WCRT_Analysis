package org.cap.model;

import java.util.HashMap;
import java.util.List;
import java.util.Queue;

public class ScheduleCacheData {
    private List<Core> cores;

    private List<Queue<Task>> queues;
    private HashMap<Integer, Long> wcrtMap;
    private CFSSimulationState simulationState;
    private long time;
    private List<Task> minRuntimeTasks;
    private int coreIndex;
    private HashMap<Integer, ScheduleCacheData> subScheduleMap;

    public List<Core> getCores() {
        return cores;
    }

    public List<Queue<Task>> getQueues() {
        return queues;
    }

    public HashMap<Integer, Long> getWcrtMap() {
        return wcrtMap;
    }

    public CFSSimulationState getSimulationState() {
        return simulationState;
    }

    public long getTime() {
        return time;
    }

    public List<Task> getMinRuntimeTasks() {
        return minRuntimeTasks;
    }

    public int getCoreIndex() {
        return coreIndex;
    }

    public HashMap<Integer, ScheduleCacheData> getSubScheduleMap() {
        return subScheduleMap;
    }

    public ScheduleCacheData(List<Core> cores, List<Queue<Task>> queues, HashMap<Integer, Long> wcrtMap,
            CFSSimulationState simulationState, long time, List<Task> minRuntimeTasks, int coreIndex) {
        this.queues = queues;
        this.wcrtMap = wcrtMap;
        this.simulationState = simulationState;
        this.time = time;
        this.minRuntimeTasks = minRuntimeTasks;
        this.coreIndex = coreIndex;
        this.subScheduleMap = new HashMap<Integer, ScheduleCacheData>();
        this.cores = cores;
    }
}
