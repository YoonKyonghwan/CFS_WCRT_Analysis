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

    public RunQueue(List<ComparatorCase> comparatorCaseList) throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        this.taskComparator = new MultiComparator();

        for (ComparatorCase compareCase :  comparatorCaseList) {
            Class<?> clazz = Class.forName(ComparatorCase.class.getPackageName() + "." + compareCase.getClassName());
            Constructor<?> ctor = clazz.getConstructor();
            this.taskComparator.insertComparator((TaskStatComparator) ctor.newInstance(new Object[] {}));
        }
        this.queueInCore = new PriorityQueue<>(this.taskComparator);
    }

    public RunQueue(MultiComparator comparator) {
        this.taskComparator = comparator;
        this.queueInCore = new PriorityQueue<>(this.taskComparator );
    }
    
    private void add(TaskStat taskStat) {
        this.queueInCore.add(taskStat);
    }

    public void addIntoQueue(TaskStat task, long time) {
        task.setQueueInsertTime(time);
        this.queueInCore.add(task);
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

    public TaskStat poll() {
        return this.queueInCore.poll();
    }

    public void clear() {
        this.queueInCore.clear();
    }

    public Stream<TaskStat> stream () {
        return this.queueInCore.stream();
    }

    public RunQueue copy() {
        RunQueue newQueue = new RunQueue(this.taskComparator);
        for (TaskStat task : this.queueInCore) {
            newQueue.add(task.copy());
        }
        return newQueue;
    }
    
    public List<TaskStat> popCandidateTasks(boolean popAll) {
        List<TaskStat> candidateTasks = new ArrayList<>();

        if (!queueInCore.isEmpty()) {
            TaskStat frontTask = this.queueInCore.peek();
            if (popAll == false) {
                candidateTasks.add(this.queueInCore.poll());
            } else {
                while (!queueInCore.isEmpty() && this.taskComparator.compare(frontTask, this.queueInCore.peek()) == 0) {
                    candidateTasks.add(queueInCore.poll());
                }
            }
        }

        return candidateTasks;
    }

    public void addAll(List<TaskStat> equalPriorityTasks) {
        this.queueInCore.addAll(equalPriorityTasks);
    }
}
