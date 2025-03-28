package org.cap.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.cap.simulation.comparator.*;

public class ScheduleCacheData {
    private List<RunQueue> queues;
    private SimulationState simulationState;
    private long time;
    private List<TaskStat> comparedTieTasks;
    private int coreIndex;
    private HashSet<Integer> subScheduleSet;

    public List<RunQueue> getQueues() {
        return queues;
    }

    public SimulationState getSimulationState() {
        return simulationState;
    }

    public long getTime() {
        return time;
    }

    public List<TaskStat> getEqualPriorityTasks() {
        return comparedTieTasks;
    }

    public int getCoreIndex() {
        return coreIndex;
    }

    public HashSet<Integer> getSubScheduleSet() {
        return subScheduleSet;
    }

    public ScheduleCacheData copy(MultiComparator comparator) throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        ScheduleCacheData scheduleData = new ScheduleCacheData(this.queues, this.simulationState, this.time, this.comparedTieTasks, this.coreIndex,
            comparator, true);

        return scheduleData;
    }

    public ScheduleCacheData(List<RunQueue> queues, HashMap<Integer, Long> wcrtMap,
            SimulationState simulationState, long time, List<TaskStat> minRuntimeTasks, int coreIndex) {
        this.queues = queues;
        this.simulationState = simulationState;
        this.time = time;
        this.comparedTieTasks = minRuntimeTasks;
        this.coreIndex = coreIndex;
        this.subScheduleSet = new HashSet<Integer>();
    }

    private List<RunQueue> copyQueues(List<RunQueue> originalQueues) {
        List<RunQueue> newQueues = new ArrayList<>();

        for (RunQueue originalQueueInCore : originalQueues) {
            RunQueue newQueueInCore = originalQueueInCore.copy();
            newQueues.add(newQueueInCore);
        }

        return newQueues;
    }

    public ScheduleCacheData(List<RunQueue> queues, SimulationState simulationState, long time, List<TaskStat> minRuntimeTasks, int coreIndex,
            MultiComparator comparator, boolean copyData)
            throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        this.time = time;
        this.coreIndex = coreIndex;
        if(copyData == true) {
            this.queues = copyQueues(queues);
            this.simulationState = simulationState.copy();
            this.comparedTieTasks = new ArrayList<>();
            for (TaskStat task : minRuntimeTasks) {
                this.comparedTieTasks.add(task.copy());
            }
            this.subScheduleSet = new HashSet<Integer>();
        } else {
            this.queues = queues;
            
            this.simulationState = simulationState;            
            this.comparedTieTasks = minRuntimeTasks;
            this.subScheduleSet = new HashSet<Integer>();
        }
    }
}
