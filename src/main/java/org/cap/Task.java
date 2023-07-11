package org.cap;

public class Task {
    int id;
    int startTime;
    double WCET;
    int nice;
    int period;
    double originalWCET;
    int currentPeriodStart;
    double priorityWeight;

    public int getPeriod() {
        return period;
    }

}