package org.cap.simulation;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.logging.Level;

import org.cap.model.CoreState;
import org.cap.model.ScheduleSimulationMethod;
import org.cap.model.SimulationState;
import org.cap.model.Task;
import org.cap.model.TaskStat;
import org.cap.simulation.comparator.ComparatorCase;

public class CFSSimulator extends DefaultSchedulerSimulator {
    private final long schedulePeriod; 
    private final long targetLatency;
    private long wakeupGranularity = 3 * 1000 * 1000L;    

    public CFSSimulator(ScheduleSimulationMethod method, List<String> comparatorCaseList, int targetLatency,
            int minimumGranularity, int wakeupGranularity, long numOfTryToSchedule, boolean initialOrder, int scheduling_tick_us)
            throws NoSuchMethodException, SecurityException, ClassNotFoundException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        super(method, minimumGranularity, numOfTryToSchedule, initialOrder);
        
        this.schedulePeriod = scheduling_tick_us * 1000L;

        this.targetLatency = targetLatency * 1000L;
        this.wakeupGranularity = wakeupGranularity * 1000L;
        ArrayList<ComparatorCase> comparatorList = new ArrayList<>();

        comparatorList.add(ComparatorCase.VIRTUAL_RUNTIME);
        for(String caseCompare : comparatorCaseList) {
            comparatorList.add(ComparatorCase.fromValue(caseCompare));
        }
        initializeRunQueue(comparatorList);
    }

    @Override
    protected long checkTaskAdditionalRuntime(TaskStat task, CoreState coreState, Queue<TaskStat> queueInCore, SimulationState simulationState, long time) {
        long remainedTime = 0;
        if(this.wakeupGranularity > 0) {
            long vruntimeWakeup = (this.wakeupGranularity << 10L)  / task.task.weight;
            if(task.virtualRuntime <= queueInCore.peek().virtualRuntime + vruntimeWakeup)  {
                remainedTime = Math.min(coreState.remainingRuntime, this.wakeupGranularity);
            }
        }
        return remainedTime;
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
            taskStat.virtualRuntime = this.targetLatency / 2;
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
        long min_vruntime = coreState.minimumVirtualRuntime - (this.targetLatency/2);  // place_entity in linux/kernel/sched/fair.c  (assume that GENTLE_FAIR_SLEEPERS is enabled)
        //long min_vruntime = coreState.minimumVirtualRuntime;  // place_entity in linux/kernel/sched/fair.c  (assume that GENTLE_FAIR_SLEEPERS is enabled)
        taskStat.virtualRuntime = Math.max(coreState.getLastVirtualRuntime(task.id), min_vruntime);
        skipReadStageIfNoReadTime(taskStat);

        return taskStat;
    }

    @Override
    protected TaskStat updateTaskStatAfterRun(TaskStat task, Queue<TaskStat> queueInCore, long timeUpdated, long remainedTime, SimulationState simulationState) {
        long vruntime_increment = (timeUpdated << 10L)  / task.task.weight;
        task.virtualRuntime += vruntime_increment;
        logger.log(Level.FINE, "Task {0} spends {1} ns from {2} to {3}[vruntime_increment: {4}]", new Object[]{task.task.id, timeUpdated, simulationState.getPreviousEventTime(), simulationState.getPreviousEventTime() + timeUpdated, vruntime_increment});
        return task;
    }

    @Override
    protected long getTimeSlice(TaskStat task, Queue<TaskStat> queueInCore) {
        long timeSlice;
        long totalWeight = queueInCore.stream().mapToLong(t -> t.task.weight).sum() + task.task.weight;
        timeSlice = Math.max(this.targetLatency * task.task.weight / totalWeight, this.minimumGranularity);
        timeSlice = (timeSlice / this.schedulePeriod) * this.schedulePeriod + this.schedulePeriod;
        timeSlice = Math.min(timeSlice, (long) (task.readTimeInNanoSeconds + task.bodyTimeInNanoSeconds + task.writeTimeInNanoSeconds));

        return timeSlice;
    }
}
