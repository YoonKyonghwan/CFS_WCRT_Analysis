package org.cap.model;

import com.google.gson.annotations.Expose;

import java.util.List;
import java.util.ArrayList;

public class Core {
    @Expose
    public int id;

    @Expose
    public List<Task> tasks;

    public Core copy() {
        Core newCore = new Core();
        newCore.id = this.id;
        newCore.tasks = new ArrayList<>(this.tasks.size());
        for (Task task : this.tasks) {
            newCore.tasks.add(task.copy());
        }
        return newCore;
    }

    public Core(int id, List<Task> tasks) {
        this.id = id;
        this.tasks = tasks;
    }

    public Core() {
    }

}
