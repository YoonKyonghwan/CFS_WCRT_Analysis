package org.cap.model;

public class SchedulePickResult {
    private ScheduleCacheData scheduleData;
    private int divergeIndex;
    private String scheduleId;
    
    public String getScheduleId() {
        return scheduleId;
    }

    public ScheduleCacheData getScheduleData() {
        return scheduleData;
    }

    public int getDivergeIndex() {
        return divergeIndex;
    }

    public SchedulePickResult(ScheduleCacheData scheduleData, int divergeIndex, String scheduleId) {
        this.scheduleData = scheduleData;
        this.divergeIndex = divergeIndex;
        this.scheduleId = scheduleId;
    }
}
