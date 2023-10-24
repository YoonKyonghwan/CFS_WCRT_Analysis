package org.cap;

// public class Main {
//     public String getGreeting() {
//         return "Hello World!";
//     }

//     public static void main(String[] args) {
//         System.out.println(new Main().getGreeting());
//     }
// }


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

import javax.annotation.Syntax;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;

public class Main {
    private static final int maxNumThreads = 8;
    // private static final int numberOfTasks = 2;
    // private static final double cpuUtilization = 0.90;

    public static void main(String[] args) {
        //parse arguments
        ArgumentParser parser = ArgumentParsers.newFor("Main").build()
                .defaultHelp(true)
                .description("Simulate CFS and PFS schedulers");
        parser.addArgument("--gen_tasks", "-gt").help("generate tasks and exit").setDefault(false);
        parser.addArgument("--num_tasksets", "-ns").help("number of taskset to generate");
        parser.addArgument("--num_tasks", "-nt").help("number of tasks in a taskset");
        parser.addArgument("--cpu_utilization", "-u").help("cpu utilization of tasks");
        Namespace params = parser.parseArgsOrFail(args);
        
        System.out.println("Hello World!");
        System.out.println();



        // if --gen_tasks is specified, generate tasks and exit
        if (params.getBoolean("gen_tasks") == true) {
            assert params.get("num_tasksets") != null && params.get("num_tasks") != null && params.get("cpu_utilization") != null: "num_tasksets, num_tasks, and cpu_utilization should be specified";
            int numberOfTasksets = Integer.parseInt(params.get("num_tasksets"));
            int numberOfTasks = Integer.parseInt(params.get("num_tasks"));
            double cpuUtilization = Double.parseDouble(params.get("cpu_utilization"));
            System.out.println("Generating tasks...");
            // String fileName = jsonTaskCreator.generateFile(numberOfTasks, cpuUtilization);
            return;
        }

        // // filepath should be specified
        // if (args.length == 0) {
        //     System.out.println("Please specify the file path");
        //     return;
        // }
    
    
        // JsonReader jsonReader = new JsonReader();
        // List<Core> cores = jsonReader.readTasksFromFile(filePath);
        // analyze_by_CFS_simulator(cores);
    
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

        long startTime = System.nanoTime();
        boolean schedulability = CFSSimulator.simulateCFS(cores).schedulability;
        long duration = (System.nanoTime() - startTime)/1000;
        System.out.println("Time consumption (CFS simulator): " + duration + " us");

        if (schedulability) {
            System.out.println("All tasks are schedulable");
        } else {
            System.out.println("Not all tasks are schedulable");
        }
    }


    private static void analyze_all_combinations_by_CFS_simulator(List<Core> cores) {
        LoggerUtility.initializeLogger();
        LoggerUtility.addConsoleLogger();

        CFSSimulator CFSSimulator = new CFSSimulator();

        long startTime = System.nanoTime();
        boolean schedulability = true;
        List<List<Core>> possibleCores = CombinationUtility.generatePossibleCores(cores);
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

        long duration = (System.nanoTime() - startTime)/1000;
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