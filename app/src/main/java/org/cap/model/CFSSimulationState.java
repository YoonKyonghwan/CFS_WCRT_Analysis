package org.cap.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Map.Entry;

public class CFSSimulationState {
    public BlockingPolicy blockingPolicy = BlockingPolicy.NONE;
    public int blockingTaskId;
    public boolean blockingPolicyReset = false;
    public List<CoreState> coreStates;
    private HashMap<Long, Integer> eventTimeMap;
    private PriorityQueue<Long> eventQueue;
    private Long previousEventTime;
    private int selectedDivergeIndex;
    private String simulationScheduleID;
    
    public String getSimulationScheduleID() {
        return simulationScheduleID;
    }

    public void setSimulationScheduleID(String simulationScheduleID) {
        this.simulationScheduleID = simulationScheduleID;
    }

    public void setSelectedDivergeIndex(int selectedDivergeIndex) {
        this.selectedDivergeIndex = selectedDivergeIndex;
    }

    public int getSelectedDivergeIndex() {
        return selectedDivergeIndex;
    }

    public CFSSimulationState copy() {
        CFSSimulationState newState = new CFSSimulationState();
        newState.blockingPolicy = this.blockingPolicy;
        newState.blockingTaskId = this.blockingTaskId;
        newState.coreStates = new ArrayList<>(this.coreStates.size());
        newState.previousEventTime = this.previousEventTime;
        newState.selectedDivergeIndex = this.selectedDivergeIndex;
        newState.simulationScheduleID = this.simulationScheduleID;
        for (CoreState coreState : this.coreStates) {
            newState.coreStates.add(coreState.copy());
        }
        newState.eventQueue = new PriorityQueue<Long>();
        for (Long value : this.eventQueue) {
            newState.eventQueue.add(Long.valueOf(value.longValue()));
        }
        newState.eventTimeMap = new HashMap<Long, Integer>();
        for (Entry<Long, Integer> entry : this.eventTimeMap.entrySet()) {
            newState.eventTimeMap.put(Long.valueOf(entry.getKey().longValue()), Integer.valueOf(entry.getValue().intValue()));
        }

        return newState;
    }

    public long getPreviousEventTime() {
        return previousEventTime;
    }

    public void setPreviousEventTime(long previousEventTime) {
        this.previousEventTime = previousEventTime;
    }

    public void insertPeriodsAtStartTime(List<Core> cores) {
        for(Core core : cores) {
            for (Task task : core.tasks) {
                putEventTime(task.startTime);
            }
        }
    }

    public void insertNextPeriodsBeforeSimulationTime(List<Core> cores, long previousEventTime, long simulationTime) {
        for(Core core : cores) {
            for (Task task : core.tasks) {
                long time = Math.max(task.startTime, task.startTime + (task.period * ((previousEventTime - task.startTime)/task.period)));
                while(time < simulationTime) {
                    if(time > previousEventTime) {
                        putEventTime(time);
                        break;
                    }
                    time += task.period;
                }
            }
        }
    }

    // insert events in ( fromTime < eventTime <= toTime)
    public void insertPeriodsInTimeRange(List<Core> cores, long fromTime, long toTime) {
        for(Core core : cores) {
            for (Task task : core.tasks) {
                long time = Math.max(task.startTime, task.startTime + (task.period * ((fromTime - task.startTime)/task.period)));
                while(time <= toTime) {
                    if(time > fromTime) {
                        putEventTime(time);
                    }
                    time += task.period;
                }
            }
        }
    }

    public void insertPeriodsIntoEventQueue(long hyperperiod, List<Core> cores) {
        for(Core core : cores) {
            for (Task task : core.tasks) {
                long time = 0;
                while(time < hyperperiod) {
                    putEventTime(time);
                    time += task.period;
                }
            }
        }
    }

    public void putEventTime(long time) {
        Long timeLong = Long.valueOf(time);
        if (this.eventTimeMap.containsKey(timeLong) == false) {
            this.eventTimeMap.put(timeLong, Integer.valueOf(1));
            this.eventQueue.add(timeLong);
            //System.out.println("time is added at " + time);
        }
        else { // this.eventTimeMap.containsKey(timeInt) == true
            this.eventTimeMap.put(timeLong, Integer.valueOf(eventTimeMap.get(timeLong).intValue() + 1));
        }
    }

    public void clearEventTime(long time) {
        Long timeLong = Long.valueOf(time);
        if (this.eventTimeMap.containsKey(timeLong) == true) {
            this.eventTimeMap.put(timeLong, Integer.valueOf(eventTimeMap.get(timeLong).intValue() - 1));
        }
    }

    public long peekNextEventTime() {
        long nextTime = -1;
        while(this.eventQueue.size() > 0) {
            Long nextTimeObj = this.eventQueue.peek();
            if(this.eventTimeMap.get(nextTimeObj).longValue() == 0L) {
                this.eventTimeMap.remove(nextTimeObj);
                nextTimeObj = this.eventQueue.poll();
            } else {
                nextTime = nextTimeObj.longValue();
                break;
            }
        }

        return nextTime;
    }
    
    public long getNextEventTime() {
        long nextTime = -1;
        while(this.eventQueue.size() > 0) {
            Long nextTimeObj = this.eventQueue.poll();
            if(this.eventTimeMap.get(nextTimeObj).intValue() > 0) {
                this.eventTimeMap.remove(nextTimeObj);
                nextTime = nextTimeObj.longValue();
                break;
            }
        }

        return nextTime;
    }

    public CFSSimulationState(int numberOfCores) {
        this.eventQueue = new PriorityQueue<Long>(); // default is ascending order
        this.eventTimeMap = new HashMap<Long, Integer>();
        this.selectedDivergeIndex = 0;
        this.simulationScheduleID = "-";
        this.coreStates = new ArrayList<>();

        for(int i = 0 ; i < numberOfCores ; i++) {
            CoreState coreState = new CoreState();
            this.coreStates.add(coreState);
        }
    }

    public CFSSimulationState() {
    }
}

