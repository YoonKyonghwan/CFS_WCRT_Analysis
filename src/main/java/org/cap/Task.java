package org.cap;

public class Task {
    int id;
    int startTime;
    double WCET;
    int nice;
    int period;
    double originalWCET; // TODO add description for below fields
    int currentPeriodStart;
    double priorityWeight;

    public int getPeriod() {
        return period;
    }

}