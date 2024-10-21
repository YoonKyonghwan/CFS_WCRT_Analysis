package org.cap.simulation;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.cap.model.CoreState;
import org.cap.model.RunQueue;
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
        this.comparatorCaseList = new ArrayList<>();
        this.comparatorCaseList.add(ComparatorCase.VIRTUAL_DEADLINE);
        for(String caseCompare : comparatorCaseList) {
            this.comparatorCaseList.add(ComparatorCase.fromValue(caseCompare));
        }
    }

    @Override
    protected long checkTaskAdditionalRuntime(TaskStat task, CoreState coreState, RunQueue queueInCore, SimulationState simulationState, long time) {
        return 0;
    }

    @Override
    protected void updateMinimumVirtualRuntime(CoreState coreState, RunQueue queue) {
        if (!queue.isEmpty())
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
        taskStat.virtualDeadline = taskStat.virtualRuntime + (this.minimumGranularity / taskStat.task.weight);
        skipReadStageIfNoReadTime(taskStat);

        return taskStat;
    }

    @Override
    protected long getTimeSlice(TaskStat task, RunQueue queueInCore) {
        long timeSlice;
        timeSlice = this.minimumGranularity;
        timeSlice = Math.min(timeSlice, (long) (task.readTimeInNanoSeconds + task.bodyTimeInNanoSeconds + task.writeTimeInNanoSeconds));
        return timeSlice;
    }

    @Override
    protected TaskStat updateTaskStatAfterRun(TaskStat task, RunQueue queueInCore, long timeUpdated, long remainedTime, SimulationState simulationState) {
        long vruntime_increment = (timeUpdated << 10L)  / task.task.weight;
        task.virtualRuntime += vruntime_increment;
        task.virtualDeadline = task.virtualRuntime + (this.minimumGranularity / task.task.weight);
        logger.log(Level.FINE, "Task {0} spends {1} ns from {2} to {3}[vruntime_increment: {4}]", new Object[]{task.task.id, timeUpdated, simulationState.getPreviousEventTime(), simulationState.getPreviousEventTime() + timeUpdated, vruntime_increment});
        return task;
    }
}
