package org.cap;

import org.cap.model.Task;
import org.cap.simulation.CFSSimulator;
import org.cap.utility.JsonReader;

import java.util.*;

public class Main {

    public static void main(String[] args) {
        JsonReader jsonReader = new JsonReader();
        List<Task> tasks = jsonReader.readTasksFromFile("tasks.json");
        CFSSimulator cfsSimulator = new CFSSimulator();
        cfsSimulator.simulateCFS(tasks);
    }
}