package org.cap;

import org.cap.model.Core;
import org.cap.simulation.CFSSimulator;
import org.cap.utility.JsonReader;

import java.util.*;

public class Main {

    public static void main(String[] args) {
        JsonReader jsonReader = new JsonReader();
        List<Core> cores = jsonReader.readTasksFromFile("tasks.json");
        CFSSimulator cfsSimulator = new CFSSimulator();
        cfsSimulator.simulateCFS(cores);
    }
}