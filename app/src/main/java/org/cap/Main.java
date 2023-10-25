package org.cap;


import org.cap.model.Core;
import org.cap.model.SimulationResult;
import org.cap.simulation.CFSAnalyzer;
import org.cap.simulation.CFSSimulator;
import org.cap.simulation.PFSSimulator;
import org.cap.utility.AnalysisResultSaver;
import org.cap.utility.ArgParser;
import org.cap.utility.CombinationUtility;
import org.cap.utility.JsonReader;
import org.cap.utility.JsonTaskCreator;
import org.cap.utility.LoggerUtility;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.sourceforge.argparse4j.inf.Namespace;

public class Main {
    private static final int maxNumThreads = 8;
    private static final int targetLatency = 30; 

    public static void main(String[] args) {
        //parse arguments
        Namespace params = new ArgParser().parseArgs(args);
        assert params.getBoolean("gen_tasks") != null || params.getString("task_info_path") != null:
            "Please specify either --gen_tasks or --task_info_path";

        // if --gen_tasks is specified, generate tasks and exit
        if (params.getBoolean("gen_tasks")) {
            JsonTaskCreator jsonTaskCreator = new JsonTaskCreator();
            jsonTaskCreator.run(params);
            return;
        }

        if (params.getString("task_info_path") != null){
            assert params.getString("result_dir") != null:
                "Please specify --resultDir to store result files";

            // if --task_info_path is specified, read tasks from file
            String taskInfoPath = params.getString("task_info_path");
            String resultDir = params.getString("result_dir");
            
            JsonReader jsonReader = new JsonReader();
            List<Core> cores = jsonReader.readTasksFromFile(taskInfoPath);
            // analyze_by_CFS_simulator(cores);
            // analyze_by_proposed(cores, targetLatency);

            // for test
            boolean simulator_schedulability = true;
            int simulator_timeConsumption = 0;
            boolean proposed_schedulability = true;
            int proposed_timeConsumption = 0;

            // save analysis results into file
            AnalysisResultSaver analysisResultSaver = new AnalysisResultSaver();
            analysisResultSaver.saveResultSummary(resultDir, taskInfoPath, simulator_schedulability, simulator_timeConsumption,
                    proposed_schedulability, proposed_timeConsumption);
        }
        


        return;
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

    private static void analyze_by_proposed(List<Core> cores, int targetLatency) {
        CFSAnalyzer analyzer = new CFSAnalyzer(cores, targetLatency);

        long startTime = System.nanoTime();
        // boolean schedulability = analyzer.analyze_parallel(cores, true, true);
        analyzer.analyze();
        long duration = (System.nanoTime() - startTime)/1000;
        System.out.println("Time consumption (proposed): " + duration + " us");

        if (analyzer.checkSchedulability()) {
            System.out.println("All tasks are schedulable");
        } else {
            System.out.println("Not all tasks are schedulable");
        }
    }
}