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
    private List<Queue<TaskStat>> queues;
    private CFSSimulationState simulationState;
    private long time;
    private List<TaskStat> minRuntimeTasks;
    private int coreIndex;
    private HashSet<Integer> subScheduleSet;

    public List<Queue<TaskStat>> getQueues() {
        return queues;
    }

    public CFSSimulationState getSimulationState() {
        return simulationState;
    }

    public long getTime() {
        return time;
    }

    public List<TaskStat> getMinRuntimeTasks() {
        return minRuntimeTasks;
    }

    public int getCoreIndex() {
        return coreIndex;
    }

    public HashSet<Integer> getSubScheduleSet() {
        return subScheduleSet;
    }

    public ScheduleCacheData copy(BasicTaskComparator comparator) throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        ScheduleCacheData scheduleData = new ScheduleCacheData(this.queues, this.simulationState, this.time, this.minRuntimeTasks, this.coreIndex,
            comparator, true);

        return scheduleData;
    }

    public ScheduleCacheData(List<Queue<TaskStat>> queues, HashMap<Integer, Long> wcrtMap,
            CFSSimulationState simulationState, long time, List<TaskStat> minRuntimeTasks, int coreIndex) {
        this.queues = queues;
        this.simulationState = simulationState;
        this.time = time;
        this.minRuntimeTasks = minRuntimeTasks;
        this.coreIndex = coreIndex;
        this.subScheduleSet = new HashSet<Integer>();
    }

    private List<Queue<TaskStat>> copyQueues(List<Queue<TaskStat>> originalQueues, BasicTaskComparator comparator)
            throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        List<Queue<TaskStat>> newQueues = new ArrayList<>();

        for (Queue<TaskStat> originalQueueInCore : originalQueues) {
            Queue<TaskStat> newQueueInCore = new PriorityQueue<>(comparator);
            // Since it clones the queue, we must not change the queue insert time
            for (TaskStat task : originalQueueInCore) {
                newQueueInCore.add(task.copy());
            }
            newQueues.add(newQueueInCore);
        }

        return newQueues;
    }

    public ScheduleCacheData(List<Queue<TaskStat>> queues, CFSSimulationState simulationState, long time, List<TaskStat> minRuntimeTasks, int coreIndex,
            BasicTaskComparator comparator, boolean copyData)
            throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        this.time = time;
        this.coreIndex = coreIndex;
        if(copyData == true) {
            this.queues = copyQueues(queues, comparator);
            this.simulationState = simulationState.copy();
            this.minRuntimeTasks = new ArrayList<>();
            for (TaskStat task : minRuntimeTasks) {
                this.minRuntimeTasks.add(task.copy());
            }
            this.subScheduleSet = new HashSet<Integer>();
        } else {
            this.queues = queues;
            
            this.simulationState = simulationState;            
            this.minRuntimeTasks = minRuntimeTasks;
            this.subScheduleSet = new HashSet<Integer>();
        }
    }
}
