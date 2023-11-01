package org.cap.model;

import com.google.gson.annotations.Expose;

import java.util.List;
import java.util.ArrayList;

public class Core {
    @Expose
    public int coreID;

    @Expose
    public List<Task> tasks;

    public double minWeight;

    public Core copy() {
        Core newCore = new Core();
        newCore.coreID = this.coreID;
        newCore.tasks = new ArrayList<>(this.tasks.size());
        newCore.minWeight = this.minWeight;
        for (Task task : this.tasks) {
            newCore.tasks.add(task.copy());
        }
        return newCore;
    }

    public Core(int id, List<Task> tasks) {
        this.coreID = id;
        this.tasks = tasks;
    }

    public Core() {
    }

}
