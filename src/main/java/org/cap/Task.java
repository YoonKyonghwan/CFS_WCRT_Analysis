package org.cap;

public class Task {
    int id;
    int startTime;
    int WCET;
    int nice;
    int period;
    int originalWCET;
    int currentPeriodStart;
    double priorityWeight;

    public int getPeriod() {
        return period;
    }

}