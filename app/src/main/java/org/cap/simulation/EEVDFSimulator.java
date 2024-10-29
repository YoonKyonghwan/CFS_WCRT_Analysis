package org.cap.simulation;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.cap.model.CoreState;
import org.cap.model.RunQueue;
import org.cap.model.ScheduleSimulationMethod;
import org.cap.model.SimulationState;
import org.cap.model.Stage;
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
        this.needsEligibleCheck = true;
    }

    @Override
    protected long checkTaskAdditionalRuntime(TaskStat task, CoreState coreState, RunQueue queueInCore, SimulationState simulationState, long time) {
        return 0;
    }

    @Override
    protected void updateMinimumVirtualRuntime(CoreState coreState, RunQueue queue) {
        coreState.minimumVirtualRuntime = Math.max(coreState.minimumVirtualRuntime, queue.getMinVruntimeInQueue());
    }

    @Override
    protected TaskStat initializeTaskStat(Task task, int targetTaskID) {
        TaskStat taskStat = new TaskStat(task);
        taskStat.readReleaseTime = task.startTime;
        if (targetTaskID == task.id){
            taskStat.virtualRuntime = 0L; // TODO
        }else{
            taskStat.virtualRuntime = 0L;
        }
        taskStat.virtualDeadline = taskStat.virtualRuntime + ((this.minimumGranularity / taskStat.task.weight) >> 1);
        skipReadStageIfNoReadTime(taskStat);

        return taskStat;
    }

    @Override
    protected TaskStat initializeWakeupTaskStat(Task task, CoreState coreState, RunQueue queue, long time) {
        TaskStat taskStat = new TaskStat(task);
        taskStat.readReleaseTime = time;
        long min_vruntime =  Math.max(coreState.minimumVirtualRuntime, queue.getMinVruntimeInQueue());
        // multiple tasks are inserted at the same time, what should be executed first, what is the value of average vruntime?
        long avg_vruntime = queue.getAverageVruntimeInQueue(min_vruntime);
        long vlag = avg_vruntime - taskStat.virtualRuntime;
        long avg_load = queue.getAverageLoad();
        long actual_lag;
        if (avg_load > 0) {
            actual_lag = vlag * (avg_load + task.weight) / avg_load;
        } else {
            actual_lag = 0;
        }

        taskStat.virtualRuntime = avg_vruntime - actual_lag;
        if (initialJobs(time, task)) {
            taskStat.virtualDeadline = taskStat.virtualRuntime + ((this.minimumGranularity / taskStat.task.weight) >> 1);
        } else {
            taskStat.virtualDeadline = taskStat.virtualRuntime + (this.minimumGranularity / taskStat.task.weight);
        }
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
        if (task.stage != Stage.COMPLETED) {
            task.virtualDeadline = task.virtualRuntime + (this.minimumGranularity / task.task.weight);
        }
        logger.log(Level.FINE, "Task {0} spends {1} ns from {2} to {3}[vruntime_increment: {4}][virtual_deadline: {5}]", new Object[]{task.task.id, timeUpdated, simulationState.getPreviousEventTime(), simulationState.getPreviousEventTime() + timeUpdated, vruntime_increment, task.virtualDeadline});
        return task;
    }
}
