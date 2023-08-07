package org.cap;

public class Task {
    int id;
    int startTime;
    double readTime;
    double bodyTime;
    double writeTime;
    int nice;
    int period;
    // TODO add description for below fields
    Stage stage = Stage.READ;
    double originalReadTime;
    double originalBodyTime;
    double originalWriteTime;
    int currentPeriodStart;
    double priorityWeight;
}