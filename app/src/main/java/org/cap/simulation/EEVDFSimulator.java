package org.cap.simulation;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import org.cap.model.CoreState;
import org.cap.model.ScheduleSimulationMethod;
import org.cap.model.SimulationState;
import org.cap.model.Task;
import org.cap.model.TaskStat;
import org.cap.simulation.comparator.ComparatorCase;

public class EEVDFSimulator extends DefaultSchedulerSimulator {

    public EEVDFSimulator(ScheduleSimulationMethod method, List<String> comparatorCaseList, int minimumGranularity, long numOfTryToSchedule, boolean initialOrder)
            throws NoSuchMethodException, SecurityException, ClassNotFoundException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        super(method, minimumGranularity, numOfTryToSchedule, initialOrder);
        
        ArrayList<ComparatorCase> comparatorList = new ArrayList<>();

        comparatorList.add(ComparatorCase.VIRTUAL_DEADLINE);
        for(String caseCompare : comparatorCaseList) {
            comparatorList.add(ComparatorCase.fromValue(caseCompare));
        }
        initializeRunQueue(comparatorList);
    }

    @Override
    protected long checkTaskAdditionalRuntime(TaskStat task, CoreState coreState, Queue<TaskStat> queueInCore, SimulationState simulationState, long time) {
        return 0;
    }

    @Override
    protected void updateMinimumVirtualRuntime(CoreState coreState, Queue<TaskStat> queue) {
        if (queue.size() >= 1)
            // coreState.minimumVirtualRuntime = queue.peek().virtualRuntime;
            coreState.minimumVirtualRuntime = Math.max(coreState.minimumVirtualRuntime, queue.peek().virtualRuntime);
    }

    @Override
    protected TaskStat initializeTaskStat(Task task, int targetTaskID) {
        TaskStat taskStat = new TaskStat(task);
        taskStat.readReleaseTime = task.startTime;
        if (targetTaskID == task.id){
            taskStat.virtualRuntime = 0; // TODO
        }else{
            taskStat.virtualRuntime = 0L;
        }
        
        skipReadStageIfNoReadTime(taskStat);

        return taskStat;
    }

    @Override
    protected TaskStat initializeWakeupTaskStat(Task task, CoreState coreState, long time) {
        TaskStat taskStat = new TaskStat(task);
        taskStat.readReleaseTime = time;
        long min_vruntime = coreState.minimumVirtualRuntime;  // TODO
        taskStat.virtualRuntime = Math.max(coreState.getLastVirtualRuntime(task.id), min_vruntime);
        skipReadStageIfNoReadTime(taskStat);

        return taskStat;
    }

    @Override
    protected long getTimeSlice(CoreState coreState, TaskStat task, Queue<TaskStat> queueInCore) {
        long timeSlice;
        timeSlice = this.minimumGranularity;
        timeSlice = Math.min(timeSlice, (long) (task.readTimeInNanoSeconds + task.bodyTimeInNanoSeconds + task.writeTimeInNanoSeconds));
        return timeSlice;
    }
}
