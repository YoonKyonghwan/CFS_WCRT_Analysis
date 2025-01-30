package org.cap.model;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.stream.Stream;

import org.cap.simulation.comparator.ComparatorCase;
import org.cap.simulation.comparator.MultiComparator;
import org.cap.simulation.comparator.TaskStatComparator;

public class RunQueue {
    MultiComparator taskComparator;
    PriorityQueue<TaskStat> queueInCore;
    boolean needsEligibleCheck = false;

    public RunQueue(List<ComparatorCase> comparatorCaseList, boolean needsEligibleCheck) throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        this.taskComparator = new MultiComparator();
        //this.popConditionChecker;

        for (ComparatorCase compareCase :  comparatorCaseList) {
            Class<?> clazz = Class.forName(ComparatorCase.class.getPackageName() + "." + compareCase.getClassName());
            Constructor<?> ctor = clazz.getConstructor();
            this.taskComparator.insertComparator((TaskStatComparator) ctor.newInstance(new Object[] {}));
        }
        this.queueInCore = new PriorityQueue<>(this.taskComparator);
        this.needsEligibleCheck = needsEligibleCheck;
    }

    public RunQueue(MultiComparator comparator, boolean needsEligibleCheck) {
        this.taskComparator = comparator;
        this.needsEligibleCheck = needsEligibleCheck;
        this.queueInCore = new PriorityQueue<>(this.taskComparator );
    }

    private void add(TaskStat taskStat) {
        this.queueInCore.add(taskStat);
    }

    public void addIntoQueue(TaskStat task, long time) {
        task.setQueueInsertTime(time);
        this.queueInCore.add(task);
    }

    public long getMinVruntimeInQueue() {
        long minVruntime = Long.MAX_VALUE;

        if(this.queueInCore.isEmpty()) {
            return 0;
        }

        for (TaskStat task : this.queueInCore) {
            if (minVruntime >  task.virtualRuntime) {
                minVruntime = task.virtualRuntime;
            }
        }

        return minVruntime;
    }

    public long getAverageVruntimeInQueue(long minimumVirtualRuntime) {
        long averageVruntime = 0L;
        long avgLoad = 0L;
        for (TaskStat task : this.queueInCore) {
            averageVruntime += (task.virtualRuntime - minimumVirtualRuntime) * task.task.weight;
        }
        
        avgLoad = getAverageLoad();
        if(avgLoad > 0) {
            return minimumVirtualRuntime + (averageVruntime / avgLoad);
        } else {
            return minimumVirtualRuntime + averageVruntime;
        }
    }

    public long getAverageLoad() {
        return this.queueInCore.stream().mapToLong(t -> t.task.weight).sum();
    }

    public long getTotalWeight() {
        return getAverageLoad();
    }

    public boolean checkEligible(TaskStat task, long avgVruntime, long minVruntime) {
        if (avgVruntime >= (task.virtualRuntime - minVruntime) * task.task.weight) {
            return true;
        } else {
            return false;
        }
    }
    

    public List<TaskStat> removeBlockingTasks(Stage blockStage, int curBlockingTaskId) {
        List<TaskStat> readTasks = new ArrayList<>();
        queueInCore.removeIf(t -> {
            if (t.stage == blockStage && t.task.id != curBlockingTaskId) {
                readTasks.add(t);
                return true;
            }
            return false;
        });

        return readTasks;
    }

    public void addAllIntoQueue(List<TaskStat> tasks, long time) {
        for(TaskStat task : tasks) {
            task.setQueueInsertTime(time);
        }
        this.queueInCore.addAll(tasks);
    }

    public boolean isEmpty() {
        return this.queueInCore.isEmpty();
    }
    
    public TaskStat peek() {
        return this.queueInCore.peek();
    }

    public int size() {
        return this.queueInCore.size();
    }

    public void clear() {
        this.queueInCore.clear();
    }

    public Stream<TaskStat> stream () {
        return this.queueInCore.stream();
    }

    public RunQueue copy() {
        RunQueue newQueue = new RunQueue(this.taskComparator, this.needsEligibleCheck);
        for (TaskStat task : this.queueInCore) {
            newQueue.add(task.copy());
        }
        return newQueue;
    }
    
    public List<TaskStat> popCandidateTasks(boolean popAll, long minimumVirtualRuntime) {
        List<TaskStat> candidateTasks = new ArrayList<>();
        List<TaskStat> poppedTasks = new ArrayList<>();
        long avgVruntime = 0L;

        if (!queueInCore.isEmpty()) {
            if(this.needsEligibleCheck == true) {
                minimumVirtualRuntime = Math.max(getMinVruntimeInQueue(), minimumVirtualRuntime);
                avgVruntime = getAverageVruntimeInQueue(minimumVirtualRuntime) - minimumVirtualRuntime;
            }

            while(candidateTasks.isEmpty()) {
                TaskStat frontTask = this.queueInCore.peek();
                if (popAll == false) {
                    candidateTasks.add(this.queueInCore.poll());
                } else {
                    while (!queueInCore.isEmpty() && this.taskComparator.compare(frontTask, this.queueInCore.peek()) == 0) {
                        TaskStat task = queueInCore.poll();
                        if(this.needsEligibleCheck == false || (this.needsEligibleCheck == true && checkEligible(task, avgVruntime, minimumVirtualRuntime))) {
                            candidateTasks.add(task);
                        } else {
                            poppedTasks.add(task); // not eligible tasks
                        }
                    }
                }
            }

            queueInCore.addAll(poppedTasks);
        }

        return candidateTasks;
    }

    public void addAll(List<TaskStat> equalPriorityTasks) {
        this.queueInCore.addAll(equalPriorityTasks);
    }
}
