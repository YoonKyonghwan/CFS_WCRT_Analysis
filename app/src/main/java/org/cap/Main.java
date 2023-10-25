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

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;

public class Main {
    private static final int maxNumThreads = 8;

    public static void main(String[] args) {
        //parse arguments
        Namespace params = parseArgs(args);

        // if --gen_tasks is specified, generate tasks and exit
        if (params.getBoolean("gen_tasks")) {
            generateTasksAndSaveIntoFiles(params);
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


    private static void generateTasksAndSaveIntoFiles(Namespace params) {
        assert params.getInt("num_sets") != null
                && params.getInt("num_tasks") != null 
                && params.getInt("num_cores") != null
                && params.getDouble("utilization") != null
                && params.getString("generated_files_save_dir") != null:
            "Please specify the #tasksets, #tasks, #cores, utilization and directory to store generated files";
        System.out.println("Generating tasks...");
        int numSets = params.getInt("num_sets");
        int numTasks = params.getInt("num_tasks");
        int numCores = params.getInt("num_cores");
        double utilization = params.getDouble("utilization");
        String generatedFilesSaveDir = params.getString("generated_files_save_dir");
        JsonTaskCreator jsonTaskCreator = new JsonTaskCreator();
        jsonTaskCreator.generateFile(numSets, numTasks, numCores, utilization, generatedFilesSaveDir);
    }


    private static Namespace parseArgs(String[] args) {
        ArgumentParser parser = ArgumentParsers.newFor("Main").build()
                .defaultHelp(true)
                .description("Simulate CFS and compute the proposed schedulability test");
        parser.addArgument("--genTasks", "-gt")
                .dest("gen_tasks")
                .action(Arguments.storeTrue())
                .help("generate tasks and exit");
        parser.addArgument("--num_sets", "-ns")
                .dest("num_sets")
                .type(Integer.class)
                .help("number of taskset to generate");
        parser.addArgument("--num_tasks", "-nt")
                .dest("num_tasks")
                .type(Integer.class)
                .help("number of tasks in a taskset");
        parser.addArgument("--num_cores", "-nc")
                .dest("num_cores")
                .type(Integer.class)
                .help("number of cores in a system");
        parser.addArgument("--utilization", "-u")
                .dest("utilization")
                .type(Double.class)
                .help("cpu utilization of tasks");
        parser.addArgument("--generated_files_save_dir", "-gd")
                .dest("generated_files_save_dir")
                .type(String.class)
                .help("directory to store generated files");
        parser.addArgument("--task_info_path", "-t")
                .dest("task_info_path")
                .type(String.class)
                .help("task info file path");
        Namespace params = parser.parseArgsOrFail(args);
        return params;
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