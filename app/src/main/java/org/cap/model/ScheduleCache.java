package org.cap.model;

import java.util.HashMap;
import java.util.List;
import java.util.Queue;

public class ScheduleCache {
    HashMap<String, ScheduleCacheData> scheduleMap;

    public ScheduleCache() {
        this.scheduleMap = new HashMap<String, ScheduleCacheData>();
    }

    // The coreIndex of the last schedule will be the total core number
    // 2_3133_4_0;3_5900_2_0;1_7000_3_2;
    // [prev_diverge_index]_[time]_[minRuntimeTasks.size()]_[coreIndex]; 
    private String makeScheduleId(String prefix, int recentDivergeIndex, long time, int divisionSize, int coreIndex) {
        String scheduleID = prefix + ";" + recentDivergeIndex + "_" + time + "_" + divisionSize + "_" + coreIndex;

        return scheduleID;
    }

    public String saveIntermediateScheduleData(String prefix, List<Core> cores, List<Queue<Task>> queues, HashMap<Integer, Long> wcrtMap, 
            CFSSimulationState simulationState, long time, List<Task> minRuntimeTasks, int coreIndex) {
        String scheduleId = makeScheduleId(prefix, simulationState.getSelectedDivergeIndex(), time, minRuntimeTasks.size(), coreIndex);
        if(!this.scheduleMap.containsKey(scheduleId)) {
            ScheduleCacheData scheduleData = new ScheduleCacheData(cores, queues, wcrtMap, simulationState, time, minRuntimeTasks, coreIndex);
            this.scheduleMap.put(scheduleId, scheduleData);

            if(this.scheduleMap.containsKey(prefix)) {
                this.scheduleMap.get(prefix).getSubScheduleMap().put(simulationState.getSelectedDivergeIndex(), scheduleData);
                assert this.scheduleMap.get(prefix).getSubScheduleMap().size() <= this.scheduleMap.get(prefix).getMinRuntimeTasks().size();

                // If all the schedule path is handled, the schedule cache of the prefix schedule is removed.
                if(this.scheduleMap.get(prefix).getSubScheduleMap().size() == this.scheduleMap.get(prefix).getMinRuntimeTasks().size()) {
                    this.scheduleMap.remove(prefix);
                }
            }
        }

        return scheduleId;
    }

    public void saveFinalScheduleData(String prefix, List<Core> cores, List<Queue<Task>> queues, HashMap<Integer, Long> wcrtMap, 
            CFSSimulationState simulationState, long time, List<Task> minRuntimeTasks, int coreIndex) {
        assert prefix.length() > 0 && this.scheduleMap.containsKey(prefix);

        ScheduleCacheData scheduleData = this.scheduleMap.get(prefix);
        assert !scheduleData.getSubScheduleMap().containsKey(simulationState.getSelectedDivergeIndex());
        ScheduleCacheData scheduleFullData = new ScheduleCacheData(cores, queues, wcrtMap, simulationState, time, minRuntimeTasks, coreIndex);
        scheduleData.getSubScheduleMap().put(simulationState.getSelectedDivergeIndex(), scheduleFullData);

        if(this.scheduleMap.get(prefix).getSubScheduleMap().size() == this.scheduleMap.get(prefix).getMinRuntimeTasks().size()) {
            this.scheduleMap.remove(prefix);
        }
     }

    public SchedulePickResult pickScheduleDataByEntry(String scheduleId) {
        SchedulePickResult selectedSchedule = null;
        ScheduleCacheData scheduleData = null;
        int divergeIndex = -1;

        ScheduleCacheData pickedData = this.scheduleMap.get(scheduleId);
        int randomInDivergedPath = (int) Math.random() * (pickedData.getMinRuntimeTasks().size() - pickedData.getSubScheduleMap().size());
        int taskIndex = 0;

        for(int i = 0; i < pickedData.getMinRuntimeTasks().size() ; i++) {
            if(!pickedData.getSubScheduleMap().containsKey(Integer.valueOf(i))) {
                if(taskIndex == randomInDivergedPath) {
                    divergeIndex = i;
                    scheduleData = pickedData; 
                }
                taskIndex++;
            }
        }

        if(divergeIndex != -1) {
            selectedSchedule = new SchedulePickResult(scheduleData, divergeIndex, scheduleId);
        }

        return selectedSchedule;
    }

    public SchedulePickResult pickScheduleData() {
        SchedulePickResult selectedSchedule = null;
        ScheduleCacheData scheduleData = null;
        int divergeIndex = -1;
        int index = 0;
        int randomVal = (int) Math.random() * this.scheduleMap.keySet().size();
        String selectedScheduleId = "";
        outerLoop:
        for(String scheduleID : this.scheduleMap.keySet()) {
            if(index == randomVal) {
                ScheduleCacheData pickedData = this.scheduleMap.get(scheduleID);
                int randomInDivergedPath = (int) Math.random() * (pickedData.getMinRuntimeTasks().size() - pickedData.getSubScheduleMap().size());
                int taskIndex = 0;

                for(int i = 0; i < pickedData.getMinRuntimeTasks().size() ; i++) {
                    if(!pickedData.getSubScheduleMap().containsKey(Integer.valueOf(i))) {
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

        if(divergeIndex != -1) {
            selectedSchedule = new SchedulePickResult(scheduleData, divergeIndex, selectedScheduleId);
        }

        return selectedSchedule;
    }
}
