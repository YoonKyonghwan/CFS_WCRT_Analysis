package org.cap;


import org.cap.model.Core;
import org.cap.model.ScheduleSimulationMethod;
import org.cap.model.SimulationResult;
import org.cap.model.TestConfiguration;
import org.cap.simulation.Analyzer;
import org.cap.simulation.CFSSimulator;
import org.cap.simulation.PFSSimulator;
import org.cap.utility.CombinationUtility;
import org.cap.utility.JsonReader;
import org.cap.utility.LoggerUtility;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;

public class Main {
    private static final int maxNumThreads = 8;
    // private static final int numberOfTasks = 2;
    // private static final double cpuUtilization = 0.90;

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
    
    
        JsonReader jsonReader = new JsonReader();
        TestConfiguration testConf = jsonReader.readTasksFromFile(params.getString("task_info_path"));
        analyze_by_CFS_simulator(testConf, ScheduleSimulationMethod.fromValue(params.getString("schedule_simulation_method")));
    
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
        // double utilization = params.getDouble("utilization");
        // String generatedFilesSaveDir = params.getString("generated_files_save_dir");
        // JsonTaskCreator jsonTaskCreator = new JsonTaskCreator();
        // jsonTaskCreator.generateFile(numSets, numTasks, numCores, utilization, generatedFilesSaveDir);
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
                .nargs(1)
                .help("number of taskset to generate");
        parser.addArgument("--num_tasks", "-nt")
                .dest("num_tasks")
                .type(Integer.class)
                .nargs(1)
                .help("number of tasks in a taskset");
        parser.addArgument("--num_cores", "-nc")
                .dest("num_cores")
                .type(Integer.class)
                .nargs(1)
                .help("number of cores in a system");
        parser.addArgument("--utilization", "-u")
                .dest("utilization")
                .type(Double.class)
                .nargs(1)
                .help("cpu utilization of tasks");
        parser.addArgument("--generated_files_save_dir", "-gd")
                .dest("generated_files_save_dir")
                .type(String.class)
                .nargs(1)
                .help("directory to store generated files");
        parser.addArgument("--task_info_path", "-t")
                .dest("task_info_path")
                .type(String.class)
                .nargs("?")
                .help("task info file path");
        parser.addArgument("--schedule_simulation_method", "-ssm")
                .dest("schedule_simulation_method")
                .type(Arguments.enumStringType(ScheduleSimulationMethod.class))
                .setDefault(ScheduleSimulationMethod.BRUTE_FORCE.toString())
                .nargs("?")
                .help("search method (either brute-force or priority-queue) ");
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

    private static void analyze_by_CFS_simulator(TestConfiguration testConf, ScheduleSimulationMethod scheduleMethod) {
        LoggerUtility.initializeLogger();
        LoggerUtility.addConsoleLogger();

        CFSSimulator CFSSimulator = new CFSSimulator(scheduleMethod);
        Logger logger = LoggerUtility.getLogger();

        long startTime = System.nanoTime();
        if(scheduleMethod == ScheduleSimulationMethod.PRIORITY_QUEUE) {
            for (Integer taskID : testConf.idNameMap.keySet()) {
                logger.info("Start simulation with target task " + taskID);
                boolean schedulability = CFSSimulator.simulateCFS(testConf.mappingInfo,
                        taskID.intValue()).schedulability;
                if (schedulability) {
                    logger.info("Task ID with " + taskID + " is schedulable");
                } else {
                    logger.info("Task ID with " + taskID + " is not schedulable");
                }
            }
        } else {
            boolean schedulability = CFSSimulator.simulateCFS(testConf.mappingInfo, -1).schedulability;
            if (schedulability) {
                System.out.println("All tasks are schedulable");
            } else {
                System.out.println("Not all tasks are schedulable");
            }
        }


         
         //boolean schedulability = CFSSimulator.simulateCFS(testConf.mappingInfo).schedulability;
         long duration = (System.nanoTime() - startTime)/1000;
         logger.info("Time consumption (CFS simulator - " + scheduleMethod.toString() + "): " + duration + " us");
    }


    private static void analyze_all_combinations_by_CFS_simulator(TestConfiguration testConf, ScheduleSimulationMethod scheduleMethod) {
        LoggerUtility.initializeLogger();
        LoggerUtility.addConsoleLogger();

        CFSSimulator CFSSimulator = new CFSSimulator(scheduleMethod);

        long startTime = System.nanoTime();
        boolean schedulability = true;
        List<List<Core>> possibleCores = CombinationUtility.generatePossibleCores(testConf.mappingInfo);
        ExecutorService threadsForSimulation = Executors.newFixedThreadPool(maxNumThreads);
        List<Future<SimulationResult>> results = new ArrayList<>();

        for (List<Core> possibleCore : possibleCores) {
            for(Integer taskID : testConf.idNameMap.keySet()) {
               Future<SimulationResult> futureResult = threadsForSimulation.submit(() ->
               CFSSimulator.simulateCFS(possibleCore, taskID.intValue()));
               results.add(futureResult);
            }
           
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