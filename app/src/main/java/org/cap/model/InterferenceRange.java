package org.cap.model;

import java.util.HashMap;

public class InterferenceRange {
    private long startTime;
    private long endTime;

    HashMap<Integer, Task> participatedTaskMap;

    public InterferenceRange() {
        this.startTime = 0L;
        this.endTime = 0L;
        this.participatedTaskMap = new HashMap<Integer, Task>();
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public boolean addTask(long time, Task task) {
        boolean inserted = false;
        long taskStartTime = time;
        long taskLength = task.originalBodyTime + task.originalReadTime + task.originalWriteTime;

        if(this.participatedTaskMap.size() == 0) {
            inserted = true;
            this.participatedTaskMap.put(Integer.valueOf(task.id), task);
            this.startTime = taskStartTime;
            this.endTime = taskStartTime + taskLength;
        } else {
            if((taskStartTime >= this.startTime && taskStartTime < this.endTime) || 
            (time + taskLength >= this.startTime && time + taskLength < this.endTime)) {
                this.participatedTaskMap.put(Integer.valueOf(task.id), task);

                if(this.startTime >= time) {
                    taskLength -= (this.startTime - time);
                    this.startTime = time;
                }
                this.endTime += taskLength;
                inserted = true;
            }
        }

        return inserted;
    }

    public int particiatedTaskNum() {
        return this.participatedTaskMap.size();
    }
}
