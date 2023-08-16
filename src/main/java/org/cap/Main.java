package org.cap;

import org.cap.model.Core;
import org.cap.simulation.Analyzer;
import org.cap.simulation.PFSSimulator;
import org.cap.utility.JsonReader;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        JsonReader jsonReader = new JsonReader();
        List<Core> cores = jsonReader.readTasksFromFile("tasks.json");

        analyze_by_simulator(cores);

        cores = jsonReader.readTasksFromFile("tasks.json");
        analyze_by_proposed(cores);
    }

    private static void analyze_by_simulator(List<Core> cores) {
        PFSSimulator PFSSimulator = new PFSSimulator();

        long startTime = System.nanoTime();
        boolean schedulability = PFSSimulator.simulatePFS(cores).schedulability;
        long duration = (System.nanoTime() - startTime)/1000;
        System.out.println("Time consumption (simulator): " + duration + " us");

        if (schedulability) {
            System.out.println("All tasks are schedulable");
        } else {
            System.out.println("Not all tasks are schedulable");
        }
    }

    private static void analyze_by_proposed(List<Core> cores) {
        Analyzer analyzer = new Analyzer();

        long startTime = System.nanoTime();
        boolean schedulability = analyzer.analyze_parallel(cores, true, true);
        // boolean schedulability = analyzer.analyze(cores, true, true);
        long duration = (System.nanoTime() - startTime)/1000;
        System.out.println("Time consumption (proposed): " + duration + " us");

        if (schedulability) {
            System.out.println("All tasks are schedulable");
        } else {
            System.out.println("Not all tasks are schedulable");
        }
    }
}