package org.cap;


import org.cap.model.Core;
import org.cap.model.ScheduleSimulationMethod;
import org.cap.model.SimulationResult;
import org.cap.model.TestConfiguration;
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
import java.util.logging.Logger;

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
            String taskInfoPath = params.getString("task_info_path");
            String resultDir = params.getString("result_dir");
            
            // read task info from file
            JsonReader jsonReader = new JsonReader();
            TestConfiguration testConf = jsonReader.readTasksFromFile(taskInfoPath);

            // analyze by simulator
            analyze_by_CFS_simulator(testConf, ScheduleSimulationMethod.fromValue(params.getString("schedule_simulation_method")));

            // analyze by proposed
            CFSAnalyzer analyzer = new CFSAnalyzer(testConf.mappingInfo, targetLatency);
            long startTime = System.nanoTime();
            analyzer.analyze(); //without parallel
            boolean proposed_schedulability = analyzer.checkSchedulability();
            int proposed_timeConsumption = (int)((System.nanoTime() - startTime)/1000);

            // for test
            boolean simulator_schedulability = true;
            int simulator_timeConsumption = 0;

            // save analysis results into file
            AnalysisResultSaver analysisResultSaver = new AnalysisResultSaver();
            analysisResultSaver.saveResultSummary(resultDir, taskInfoPath, simulator_schedulability, simulator_timeConsumption,
                    proposed_schedulability, proposed_timeConsumption);
        }
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
}