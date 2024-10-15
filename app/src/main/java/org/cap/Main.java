package org.cap;

import org.cap.model.GANiceAssigner;
import org.cap.model.NiceAssignMethod;
import org.cap.model.ScheduleSimulationMethod;
import org.cap.model.SimulationResult;
import org.cap.model.TestConfiguration;
import org.cap.simulation.*;
import org.cap.simulation.comparator.ComparatorCase;
import org.cap.utility.AnalysisResultSaver;
import org.cap.utility.ArgParser;
import org.cap.utility.JsonReader;
import org.cap.utility.LoggerUtility;
import org.cap.utility.MathUtility;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;

import net.sourceforge.argparse4j.inf.Namespace;

public class Main {
    public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException, SecurityException,
            InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        // parse arguments
        Namespace params = new ArgParser().parseArgs(args);
        assert params.getString("task_info_path") != null : "Please specify --task_info_path to load task info file";
        assert params.getString("result_dir") != null : "Please specify --resultDir to store result files";
        String taskInfoPath = params.getString("task_info_path");
        String resultDir = params.getString("result_dir");

        // read task info from file & initialize variables
        JsonReader jsonReader = new JsonReader();
        TestConfiguration testConf = jsonReader.readTasksFromFile(taskInfoPath);
        testConf.initializeTaskData();
        
        // analyze by proposed method
        MathUtility.convertPeriod_ns_us(testConf);
        long startTime = System.nanoTime();
        boolean proposed_schedulability = analysis_by_proposed(testConf, params);
        long proposed_timeConsumption = (System.nanoTime() - startTime) / 1000L;
        
        // analyze by simulator
        MathUtility.convertPeriod_us_ns(testConf);
        startTime = System.nanoTime();
        boolean simulator_schedulability = analyze_by_CFS_simulator(testConf, params);            
        long simulator_timeConsumption = (System.nanoTime() - startTime)/1000L; //us

        // save analysis results into file
        MathUtility.convertPeriod_ns_us(testConf);
        AnalysisResultSaver analysisResultSaver = new AnalysisResultSaver();
        analysisResultSaver.saveResultSummary(
                resultDir, taskInfoPath, 
                simulator_schedulability, simulator_timeConsumption,
                proposed_schedulability, proposed_timeConsumption
                );
        analysisResultSaver.saveDetailedResult(resultDir, taskInfoPath, testConf);

        // update the task info file with the nice values
        analysisResultSaver.updateNiceValues(taskInfoPath, testConf);
    }



    private static boolean analysis_by_proposed(TestConfiguration testConf, Namespace params) {
        NiceAssignMethod niceAssignMethod = NiceAssignMethod.fromValue(params.getString("nice_assign_method"));
        boolean proposed_schedulability = false;
        double nice_lambda = params.getDouble("nice_lambda");
        int num_tasks = testConf.mappingInfo.stream().mapToInt(core -> core.tasks.size()).sum();
        if (niceAssignMethod == NiceAssignMethod.BASELINE) {
            MathUtility.assignNiceValues(testConf.mappingInfo, nice_lambda);
            CFSAnalyzer analyzer = new CFSAnalyzer(testConf.mappingInfo, params.getInt("target_latency"), params.getInt("minimum_granularity"), params.getInt("jiffy"));
            analyzer.analyze(); 
            proposed_schedulability = analyzer.checkSystemSchedulability();
        }else if (niceAssignMethod == NiceAssignMethod.HEURISTIC){
            // for accessing the nice value assignment algorithm.
            nice_lambda = 0;      
            while(!proposed_schedulability && nice_lambda < 40) {
                MathUtility.assignNiceValues(testConf.mappingInfo, nice_lambda);
                CFSAnalyzer analyzer = new CFSAnalyzer(testConf.mappingInfo, params.getInt("target_latency"), params.getInt("minimum_granularity"), params.getInt("jiffy"));
                analyzer.analyze(); 
                proposed_schedulability = analyzer.checkSystemSchedulability();
                if (!proposed_schedulability){
                    nice_lambda = nice_lambda + 0.1;
                }
            }
            if (!proposed_schedulability){
                nice_lambda = params.getDouble("nice_lambda"); 
                MathUtility.assignNiceValues(testConf.mappingInfo, nice_lambda);
            } 
        }else{ //GA
            int num_chromosomes = 1000;
            int timeout_ms = 3000;
            double mutationRate = 0.05;
            GANiceAssigner gaNiceAssigner = new GANiceAssigner(num_chromosomes, timeout_ms, mutationRate, num_tasks, params.getInt("target_latency"), params.getInt("minimum_granularity"), params.getInt("jiffy"));
            gaNiceAssigner.evolve(testConf.mappingInfo);
            CFSAnalyzer analyzer = new CFSAnalyzer(testConf.mappingInfo, params.getInt("target_latency"), params.getInt("minimum_granularity"), params.getInt("jiffy"));
            analyzer.analyze(); 
            proposed_schedulability = analyzer.checkSystemSchedulability();
        }
        return proposed_schedulability;
    }


    private static boolean analyze_by_CFS_simulator(TestConfiguration testConf, Namespace params) throws ClassNotFoundException, NoSuchMethodException, SecurityException,
            InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        
        ScheduleSimulationMethod scheduleMethod = ScheduleSimulationMethod.fromValue(params.getString("schedule_simulation_method"));
        ComparatorCase compareCase = ComparatorCase.fromValue(params.getString("tie_comparator"));
        long schedule_try_count = params.getLong("schedule_try_count");
        String logger_option = params.getString("logger_option");
        int test_try_count = params.getInt("test_try_count");
        int targetLatency = params.getInt("target_latency");
        int minimumGranularity = params.getInt("minimum_granularity");
        int wakeupGranularity = params.getInt("wakeup_granularity");
        int scheduling_tick_us = params.getInt("jiffy");
        boolean initial_order = params.getBoolean("initial_order");
        LoggerUtility.initializeLogger(logger_option);
        LoggerUtility.addConsoleLogger();
                
        // for brute-force method, unordered comparator is used.
        if ((scheduleMethod == ScheduleSimulationMethod.BRUTE_FORCE || scheduleMethod == ScheduleSimulationMethod.RANDOM) && 
            (compareCase != ComparatorCase.RELEASE_TIME && compareCase != ComparatorCase.FIFO && compareCase != ComparatorCase.TARGET_TASK  && 
            compareCase != ComparatorCase.INITIAL_ORDER)) {
            compareCase = ComparatorCase.FIFO;
        }

        if(scheduleMethod == ScheduleSimulationMethod.RANDOM_TARGET_TASK) {
            compareCase = ComparatorCase.TARGET_TASK;
        }

        if(scheduleMethod == ScheduleSimulationMethod.BRUTE_FORCE) {
            test_try_count = 1;
        }

        CFSSimulator CFSSimulator = new CFSSimulator(scheduleMethod, compareCase, targetLatency, minimumGranularity, wakeupGranularity, schedule_try_count, initial_order, scheduling_tick_us);
        Logger logger = LoggerUtility.getLogger();
        boolean system_schedulability = true;

        long simulationTime = MathUtility.getLCM(testConf.mappingInfo) * 2;
        
        logger.info("Simulated Time (ms): " + simulationTime/1000000L);

        if (scheduleMethod == ScheduleSimulationMethod.PRIORITY_QUEUE) {
            for (Integer taskID : testConf.idNameMap.keySet()) {
                logger.fine("\n\n ********** Start simulation with target task: " + taskID + " **********");
                SimulationResult simulResult = CFSSimulator.simulateCFS(testConf.mappingInfo,
                        taskID.intValue(), simulationTime);
                int WCRT_by_simulator = (int) (simulResult.wcrtMap.get(taskID)/1000);
                CFSSimulator.findTaskbyID(testConf, taskID.intValue()).WCRT_by_simulator = WCRT_by_simulator;
                long task_period = CFSSimulator.findTaskbyID(testConf, taskID.intValue()).period/1000;
                boolean task_schedulability = (WCRT_by_simulator <= task_period);
                CFSSimulator.findTaskbyID(testConf, taskID.intValue()).isSchedulable_by_simulator = task_schedulability;

                logger.info(String.format("Task ID with %3d (WCRT: %8d us, Period: %8d us, Schedulability: %5s)", taskID, WCRT_by_simulator, task_period, task_schedulability));
                if(simulResult.schedulability == false)
                    system_schedulability = false;
            }
        } else if (scheduleMethod == ScheduleSimulationMethod.RANDOM_TARGET_TASK) {
            SimulationResult finalSimulationResult = new SimulationResult();
            long totalTryCount = 0L;
            for (Integer taskID : testConf.idNameMap.keySet()) {
                logger.fine("\n\n ********** Start simulation with target task: " + taskID + " **********");
                for(int i = 0 ; i  < test_try_count ; i++) {
                    SimulationResult simulResult = CFSSimulator.simulateCFS(testConf.mappingInfo,
                            taskID.intValue(), simulationTime);
                    CFSSimulator.mergeToFinalResult(finalSimulationResult, simulResult);
                    totalTryCount += CFSSimulator.getTriedScheduleCount();
                }
            }

            system_schedulability = finalSimulationResult.schedulability;
            for (Integer taskIDWCRT : testConf.idNameMap.keySet()) {
                long WCRT_by_simulator = (finalSimulationResult.wcrtMap.get(taskIDWCRT)/1000);
                long deadline = CFSSimulator.findTaskbyID(testConf, taskIDWCRT.intValue()).period/1000;
                boolean task_schedulability = (WCRT_by_simulator <= deadline);
                CFSSimulator.findTaskbyID(testConf, taskIDWCRT.intValue()).isSchedulable_by_simulator = task_schedulability;
                CFSSimulator.findTaskbyID(testConf, taskIDWCRT.intValue()).WCRT_by_simulator = (int) WCRT_by_simulator;
                logger.info(String.format("Task ID with %3d (WCRT: %8d us, Period: %8d us, Schedulability: %5s)", taskIDWCRT, WCRT_by_simulator, deadline, task_schedulability));
                //if(finalSimulationResult.schedulability == false)
                //    system_schedulability = false;
            }
            logger.info("Schedule execution count (unique): " + totalTryCount);
        } else{
            SimulationResult finalSimulationResult = new SimulationResult();
            long totalTryCount = 0L;
            for(int i = 0 ; i  < test_try_count ; i++) {
                SimulationResult simulResult = CFSSimulator.simulateCFS(testConf.mappingInfo, -1, simulationTime);
                CFSSimulator.mergeToFinalResult(finalSimulationResult, simulResult);
                totalTryCount += CFSSimulator.getTriedScheduleCount();
            }
            
            system_schedulability = finalSimulationResult.schedulability;
            for (Integer taskID : testConf.idNameMap.keySet()) {
                long WCRT_by_simulator = (finalSimulationResult.wcrtMap.get(taskID)/1000);
                long deadline = CFSSimulator.findTaskbyID(testConf, taskID.intValue()).period/1000;
                boolean task_schedulability = (WCRT_by_simulator <= deadline);
                CFSSimulator.findTaskbyID(testConf, taskID.intValue()).isSchedulable_by_simulator = task_schedulability;
                CFSSimulator.findTaskbyID(testConf, taskID.intValue()).WCRT_by_simulator = (int) WCRT_by_simulator;
                logger.info(String.format("Task ID with %3d (WCRT: %8d us, Period: %8d us, Schedulability: %5s)", taskID, WCRT_by_simulator, deadline, task_schedulability));
            }
            logger.info("Schedule execution count (unique): " + totalTryCount);
            if (system_schedulability) {
                logger.info("All tasks are schedulable");
            } else {
                logger.info("Not all tasks are schedulable");
            }
        }

        return system_schedulability;
    }
}
