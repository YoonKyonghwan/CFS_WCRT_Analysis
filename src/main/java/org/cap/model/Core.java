package org.cap.model;

import java.util.List;

public class Core {
    public int id;
    public List<Task> tasks;

    public Core(int id, List<Task> tasks) {
        this.id = id;
        this.tasks = tasks;
    }
}
