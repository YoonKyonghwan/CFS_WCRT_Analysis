package org.cap.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Map.Entry;

public class CFSSimulationState {

    public int targetedLatency = 20;
    public int minimumGranularity = 4;
    public BlockingPolicy blockingPolicy = BlockingPolicy.NONE;
    public int blockingTaskId;
    public boolean blockingPolicyReset = false;
    public List<CoreState> coreStates;
    private ScheduleSimulationMethod method;
    private HashMap<Integer, Integer> eventTimeMap;
    private PriorityQueue<Integer> eventQueue;
    private int previousEventTime;

    // TODO consider moving WCRTs and Queues to here after completion

    
    public CFSSimulationState copy() {
        CFSSimulationState newState = new CFSSimulationState(this.method);
        newState.targetedLatency = this.targetedLatency;
        newState.minimumGranularity = this.minimumGranularity;
        newState.blockingPolicy = this.blockingPolicy;
        newState.blockingTaskId = this.blockingTaskId;
        newState.coreStates = new ArrayList<>(this.coreStates.size());
        newState.previousEventTime = this.previousEventTime;
        for (CoreState coreState : this.coreStates) {
            newState.coreStates.add(coreState.copy());
        }
        newState.eventQueue = new PriorityQueue<Integer>();
        for (Integer value : this.eventQueue) {
            newState.eventQueue.add(Integer.valueOf(value.intValue()));
        }
        newState.eventTimeMap = new HashMap<Integer, Integer>();
        for (Entry<Integer, Integer> entry : this.eventTimeMap.entrySet()) {
            newState.eventTimeMap.put(Integer.valueOf(entry.getKey().intValue()), Integer.valueOf(entry.getValue().intValue()));
        }

        return newState;
    }

    public int getPreviousEventTime() {
        return previousEventTime;
    }

    public void setPreviousEventTime(int previousEventTime) {
        this.previousEventTime = previousEventTime;
    }

    public void insertPeriodsIntoEventQueue(long hyperperiod, List<Core> cores) {
        for(Core core : cores) {
            for (Task task : core.tasks) {
                int time = 0;
                while(time < hyperperiod) {
                    putEventTime(time);
                    time += task.period;
                }
            }
        }
    }

    public void putEventTime(int time) {
        Integer timeInt = Integer.valueOf(time);
        if (this.eventTimeMap.containsKey(timeInt) == false) {
            this.eventTimeMap.put(timeInt, Integer.valueOf(1));
            this.eventQueue.add(timeInt);
            //System.out.println("time is added at " + time);
        }
        else { // this.eventTimeMap.containsKey(timeInt) == true
            this.eventTimeMap.put(timeInt, Integer.valueOf(eventTimeMap.get(timeInt).intValue() + 1));
        }
    }

    public void clearEventTime(int time) {
        Integer timeInt = Integer.valueOf(time);
        if (this.eventTimeMap.containsKey(timeInt) == true) {
            this.eventTimeMap.put(timeInt, Integer.valueOf(eventTimeMap.get(timeInt).intValue() - 1));
        }
    }
    
    public int getNextEventTime() {
        int nextTime = -1;
        while(this.eventQueue.size() > 0) {
            Integer nextTimeObj = this.eventQueue.poll();
            if(this.eventTimeMap.get(nextTimeObj).intValue() > 0) {
                this.eventTimeMap.remove(nextTimeObj);
                nextTime = nextTimeObj.intValue();
                break;
            }
        }

        return nextTime;
    }

    public CFSSimulationState(int targetedLatency, int minimumGranularity, int numberOfCores, ScheduleSimulationMethod method) {
        this.targetedLatency = targetedLatency;
        this.minimumGranularity = minimumGranularity;
        this.method = method;
        this.eventQueue = new PriorityQueue<Integer>(); // default is ascending order
        this.eventTimeMap = new HashMap<Integer, Integer>();

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
        this.method = method;
    }
}

