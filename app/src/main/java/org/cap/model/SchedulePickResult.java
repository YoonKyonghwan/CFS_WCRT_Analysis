package org.cap.model;

public class SchedulePickResult {
    private ScheduleCacheData scheduleData;
    private int divergeIndex;
    private long scheduleId;
    
    public long getScheduleId() {
        return scheduleId;
    }

    public ScheduleCacheData getScheduleData() {
        return scheduleData;
    }

    public int getDivergeIndex() {
        return divergeIndex;
    }

    public SchedulePickResult(ScheduleCacheData scheduleData, int divergeIndex, long scheduleId) {
        this.scheduleData = scheduleData;
        this.divergeIndex = divergeIndex;
        this.scheduleId = scheduleId;
    }
}
