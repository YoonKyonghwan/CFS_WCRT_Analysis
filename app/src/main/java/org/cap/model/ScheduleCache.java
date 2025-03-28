package org.cap.model;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.Stack;

import org.cap.simulation.comparator.MultiComparator;

public class ScheduleCache {
    HashMap<String, ScheduleCacheData> scheduleMap;
    Stack<ScheduleCacheData> scheduleStateStack;
    long nextScheduleID;
    ScheduleSimulationMethod method;

    public ScheduleCache(ScheduleSimulationMethod method) {
        this.scheduleMap = new HashMap<String, ScheduleCacheData>();
        this.scheduleStateStack = new Stack<ScheduleCacheData>();
        this.nextScheduleID = 0L;
        this.method = method;
    }

    public String pushScheduleData(String parentScheduleID, List<RunQueue> queues,
            HashMap<Integer, Long> wcrtMap,
            SimulationState simulationState, long time, List<TaskStat> minRuntimeTasks, int coreIndex,
            MultiComparator comparator) throws ClassNotFoundException, NoSuchMethodException, SecurityException,
            InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        
        //long scheduleId = getNewScheduleID();
        String scheduleId = makeScheduleId(parentScheduleID, simulationState.getSelectedDivergeIndex(), time, minRuntimeTasks.size(), coreIndex);
        ScheduleCacheData scheduleData = null;
        
        if(!this.scheduleMap.containsKey(scheduleId)) {
            scheduleData = new ScheduleCacheData(queues, simulationState, time, minRuntimeTasks, coreIndex, comparator, true);
        } else {
            scheduleData = this.scheduleMap.get(scheduleId);
        }
        this.scheduleStateStack.push(scheduleData);
        
        saveScheduleDataToMap(scheduleId, parentScheduleID, scheduleData);

        return scheduleId;
    }

    public ScheduleCacheData popScheduleData() {
        return this.scheduleStateStack.pop();
    }

    public void clearStack() {
        this.scheduleStateStack.clear();
    }

    public int getScheduleStackSize() {
        return this.scheduleStateStack.size();
    }

    // The coreIndex of the last schedule will be the total core number
    // 2_3133_4_0;3_5900_2_0;1_7000_3_2;
    // [prev_diverge_index]_[time]_[minRuntimeTasks.size()]_[coreIndex]; 
    private String makeScheduleId(String prefix, int recentDivergeIndex, long time, int divisionSize, int coreIndex) {
        String scheduleID = String.format("%x;%x_%x_%x_%x", prefix.hashCode(), recentDivergeIndex, time, divisionSize, coreIndex);

        return scheduleID;
    }

    private void saveScheduleDataToMap(String scheduleId, String parentScheduleID, ScheduleCacheData scheduleData) {
        if(!this.scheduleMap.containsKey(scheduleId)) {
            this.scheduleMap.put(scheduleId, scheduleData);
        }

        if(this.scheduleMap.containsKey(parentScheduleID)) {
            this.scheduleMap.get(parentScheduleID).getSubScheduleSet().add(scheduleData.getSimulationState().getSelectedDivergeIndex());
            if(this.scheduleMap.get(parentScheduleID).getSubScheduleSet().size() > 1) {
                assert this.scheduleMap.get(parentScheduleID).getSubScheduleSet().size() <= this.scheduleMap.get(parentScheduleID).getEqualPriorityTasks().size();
            }

            // If all the schedule path is handled, the schedule cache of the prefix schedule is removed.
            if(this.scheduleMap.get(parentScheduleID).getSubScheduleSet().size() == this.scheduleMap.get(parentScheduleID).getEqualPriorityTasks().size()) {
                if(this.method != ScheduleSimulationMethod.BRUTE_FORCE) {
                    this.scheduleMap.remove(parentScheduleID);
                }
                
            }
        }
    }

    public void saveFinalScheduledIndex(String parentScheduleID, SimulationState simulationState) {
        //assert prefix.length() > 0 && this.scheduleMap.containsKey(prefix);
        ScheduleCacheData scheduleData = this.scheduleMap.get(parentScheduleID);
        
       //assert !scheduleData.getSubScheduleMap().containsKey(simulationState.getSelectedDivergeIndex());
       if(scheduleData != null) {
            scheduleData.getSubScheduleSet().add(simulationState.getSelectedDivergeIndex());

            if(this.scheduleMap.get(parentScheduleID).getSubScheduleSet().size() == this.scheduleMap.get(parentScheduleID).getEqualPriorityTasks().size()) {
                if(this.method != ScheduleSimulationMethod.BRUTE_FORCE) {
                    this.scheduleMap.remove(parentScheduleID);
                }
            }
        }
     }

    public SchedulePickResult pickScheduleDataByEntry(String scheduleId, boolean random) {
        SchedulePickResult selectedSchedule = null;
        ScheduleCacheData scheduleData = null;
        int divergeIndex = -1;
        int randomInDivergedPath;

        ScheduleCacheData pickedData = this.scheduleMap.get(scheduleId);
        
        int taskIndex = 0;

        if(random == true) {
            randomInDivergedPath = (int) (Math.random() * (pickedData.getEqualPriorityTasks().size() - pickedData.getSubScheduleSet().size()));
        } else {
            randomInDivergedPath = 0;
        }

        for(int i = 0; i < pickedData.getEqualPriorityTasks().size() ; i++) {
            if(!pickedData.getSubScheduleSet().contains(Integer.valueOf(i))) {
                if(taskIndex == randomInDivergedPath) {
                    divergeIndex = i;
                    scheduleData = pickedData;
                    break;
                }
                taskIndex++;
            }
        }

        if(divergeIndex != -1) {
            selectedSchedule = new SchedulePickResult(scheduleData, divergeIndex, scheduleId);
        }

        return selectedSchedule;
    }

    public SchedulePickResult pickScheduleData(MultiComparator comparator)
            throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        SchedulePickResult selectedSchedule = null;
        ScheduleCacheData scheduleData = null;
        int divergeIndex = -1;
        int index = 0;
        int randomVal = (int) (Math.random() * this.scheduleMap.keySet().size());
        String selectedScheduleId = "-";
        outerLoop:
        for(String scheduleID : this.scheduleMap.keySet()) {
            if(index == randomVal) {
                ScheduleCacheData pickedData = this.scheduleMap.get(scheduleID);
                int randomInDivergedPath = (int) (Math.random() * (pickedData.getEqualPriorityTasks().size() - pickedData.getSubScheduleSet().size()));
                int taskIndex = 0;

                for(int i = 0; i < pickedData.getEqualPriorityTasks().size() ; i++) {
                    if(!pickedData.getSubScheduleSet().contains(Integer.valueOf(i))) {
                        if(taskIndex == randomInDivergedPath) {
                            divergeIndex = i;
                            scheduleData = pickedData; 
                            selectedScheduleId = scheduleID;
                            break outerLoop;
                        }
                        taskIndex++;
                    }
                }
            }
            index++;
        }

        if(divergeIndex != -1 && scheduleData != null) {
            selectedSchedule = new SchedulePickResult(scheduleData.copy(comparator), divergeIndex, selectedScheduleId);
        }

        return selectedSchedule;
    }
}
