package org.cap.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.cap.simulation.comparator.*;

public class ScheduleCacheData {
    private List<Queue<Task>> queues;
    private HashMap<Integer, Long> wcrtMap;
    private CFSSimulationState simulationState;
    private long time;
    private List<Task> minRuntimeTasks;
    private int coreIndex;
    private HashSet<Integer> subScheduleSet;
    private ComparatorCase comparatorCase;

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

    public HashSet<Integer> getSubScheduleSet() {
        return subScheduleSet;
    }

    public ScheduleCacheData copy() throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        ScheduleCacheData scheduleData = new ScheduleCacheData(this.queues, this.wcrtMap,
            this.simulationState, this.time, this.minRuntimeTasks, this.coreIndex,
            this.comparatorCase, true);

        return scheduleData;
    }

    public ScheduleCacheData(List<Queue<Task>> queues, HashMap<Integer, Long> wcrtMap,
            CFSSimulationState simulationState, long time, List<Task> minRuntimeTasks, int coreIndex) {
        this.queues = queues;
        this.wcrtMap = wcrtMap;
        this.simulationState = simulationState;
        this.time = time;
        this.minRuntimeTasks = minRuntimeTasks;
        this.coreIndex = coreIndex;
        this.subScheduleSet = new HashSet<Integer>();
        this.comparatorCase = ComparatorCase.FIFO;
    }

    private List<Queue<Task>> copyQueues(List<Queue<Task>> originalQueues, ComparatorCase comparatorCase)
            throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        List<Queue<Task>> newQueues = new ArrayList<>();

        for (Queue<Task> originalQueueInCore : originalQueues) {
            Class<?> clazz = Class
                    .forName(ComparatorCase.class.getPackageName() + "." + comparatorCase.getClassName());
            Constructor<?> ctor = clazz.getConstructor();
            BasicTaskComparator taskComparator = (BasicTaskComparator) ctor.newInstance(new Object[] {});
            Queue<Task> newQueueInCore = new PriorityQueue<>(taskComparator);
            // Since it clones the queue, we must not change the queue insert time
            for (Task task : originalQueueInCore) {
                newQueueInCore.add(task.copy());
            }
            newQueues.add(newQueueInCore);
        }

        return newQueues;
    }

    private HashMap<Integer, Long> cloneHashMap(HashMap<Integer, Long> mapToBeCopied) {
        HashMap<Integer, Long> clonedMap = new HashMap<Integer, Long>();

        for (Integer key : mapToBeCopied.keySet()) {
            clonedMap.put(key, Long.valueOf(mapToBeCopied.get(key).longValue()));
        }

        return clonedMap;
    }

    public ScheduleCacheData(List<Queue<Task>> queues, HashMap<Integer, Long> wcrtMap,
            CFSSimulationState simulationState, long time, List<Task> minRuntimeTasks, int coreIndex,
            ComparatorCase comparatorCase, boolean copyData)
            throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        this.time = time;
        this.coreIndex = coreIndex;
        this.comparatorCase = comparatorCase;
        if(copyData == true) {
            this.queues = copyQueues(queues, comparatorCase);
            this.wcrtMap = cloneHashMap(wcrtMap);
            this.simulationState = simulationState.copy();
            this.minRuntimeTasks = new ArrayList<>();
            for (Task task : minRuntimeTasks) {
                this.minRuntimeTasks.add(task.copy());
            }
            this.subScheduleSet = new HashSet<Integer>();
        } else {
            this.queues = queues;
            this.wcrtMap = wcrtMap;
            this.simulationState = simulationState;            
            this.minRuntimeTasks = minRuntimeTasks;
            this.subScheduleSet = new HashSet<Integer>();
        }
    }
}
