package org.cap;

import org.cap.model.Core;
import org.cap.model.SimulationResult;
import org.cap.simulation.Analyzer;
import org.cap.simulation.CFSSimulator;
import org.cap.simulation.PFSSimulator;
import org.cap.utility.CombinationUtility;
import org.cap.utility.JsonReader;
import org.cap.utility.JsonTaskCreator;
import org.cap.utility.LoggerUtility;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Main {
    private static final int maxNumThreads = 8;
    private static final int numberOfTasks = 10;
    private static final double cpuUtilization = 0.70;

    public static void main(String[] args) {
        JsonTaskCreator jsonTaskCreator = new JsonTaskCreator();
        String fileName = jsonTaskCreator.generateFile(numberOfTasks, cpuUtilization);

        JsonReader jsonReader = new JsonReader();
        List<Core> cores = jsonReader.readTasksFromFile(fileName);
        analyze_by_CFS_simulator(cores);

        // cores = jsonReader.readTasksFromFile("tasks.json");
        // analyze_by_proposed(cores);
    }

    private static void analyze_by_PFS_simulator(List<Core> cores) {
        PFSSimulator PFSSimulator = new PFSSimulator();

        long startTime = System.nanoTime();
        boolean schedulability = PFSSimulator.simulatePFS(cores).schedulability;
        long duration = (System.nanoTime() - startTime)/1000;
        System.out.println("Time consumption (PFS simulator): " + duration + " us");

        if (schedulability) {
            System.out.println("All tasks are schedulable");
        } else {
            System.out.println("Not all tasks are schedulable");
        }
    }

    private static void analyze_by_CFS_simulator(List<Core> cores) {
        LoggerUtility.initializeLogger();
        LoggerUtility.addConsoleLogger();

        CFSSimulator CFSSimulator = new CFSSimulator();

        long startTime = System.currentTimeMillis();

        CFSSimulator.simulateCFS(cores);

        List<List<Core>> possibleCores = CombinationUtility.generatePossibleCores(cores);

        boolean schedulability = true;
        ExecutorService threadsForSimulation = Executors.newFixedThreadPool(maxNumThreads);
        List<Future<SimulationResult>> results = new ArrayList<>();

        for (List<Core> possibleCore : possibleCores) {
            Future<SimulationResult> futureResult = threadsForSimulation.submit(() ->
                CFSSimulator.simulateCFS(possibleCore));
            results.add(futureResult);
        }

        for (Future<SimulationResult> future : results) {
            try {
                SimulationResult simulationResult = future.get();
                if (!simulationResult.schedulability) {
                    System.out.println("Not all tasks are schedulable");
                    schedulability = false;
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (schedulability)
            System.out.println("All tasks are schedulable");

        threadsForSimulation.shutdown();

        long duration = (System.currentTimeMillis() - startTime);
        System.out.println("Time consumption (CFS simulator): " + duration + " ms");
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