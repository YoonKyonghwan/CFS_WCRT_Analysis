package org.cap;

public class Task {
    int id;
    int startTime;
    int originalWCET;
    int WCET;
    int nice;
    int period;
    double priorityWeight;

    public int getPeriod() {
        return period;
    }

}