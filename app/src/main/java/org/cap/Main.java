package org.cap;

import org.cap.model.ScheduleSimulationMethod;
import org.cap.model.SimulationResult;
import org.cap.model.TestConfiguration;
import org.cap.simulation.CFSAnalyzer;
import org.cap.simulation.CFSSimulator;
import org.cap.simulation.comparator.ComparatorCase;
import org.cap.utility.AnalysisResultSaver;
import org.cap.utility.ArgParser;
import org.cap.utility.JsonReader;
import org.cap.utility.JsonTaskCreator;
import org.cap.utility.LoggerUtility;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;

import net.sourceforge.argparse4j.inf.Namespace;

public class Main {
    private static final int targetLatency = 20000;  //us (20ms)

    public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException, SecurityException,
            InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        // parse arguments
        Namespace params = new ArgParser().parseArgs(args);
        assert params.getBoolean("gen_tasks") != null || params.getString("task_info_path") != null
                : "Please specify either --gen_tasks or --task_info_path";

        // if --gen_tasks is specified, generate tasks and exit
        if (params.getBoolean("gen_tasks")) {
            JsonTaskCreator jsonTaskCreator = new JsonTaskCreator();
            jsonTaskCreator.run(params);
            return;
        }

        if (params.getString("task_info_path") != null) {
            assert params.getString("result_dir") != null : "Please specify --resultDir to store result files";
            String taskInfoPath = params.getString("task_info_path");
            String resultDir = params.getString("result_dir");

            // read task info from file
            JsonReader jsonReader = new JsonReader();
            TestConfiguration testConf = jsonReader.readTasksFromFile(taskInfoPath);
            testConf.initializeTaskData();
            
            long startTime = System.nanoTime();
            
            // analyze by simulator
            boolean simulator_schedulability = analyze_by_CFS_simulator(testConf,
            ScheduleSimulationMethod.fromValue(params.getString("schedule_simulation_method")),
            ComparatorCase.fromValue(params.getString("tie_comparator")), params.getLong("simulation_time"), params.getLong("schedule_try_count"), params.getString("logger_option"));
            int simulator_timeConsumption = (int)((System.nanoTime() - startTime)/1000); //us
            // System.out.println("Time consumption (CFS simulator): " + simulator_timeConsumption + " us");
            
            // analyze by proposed
            startTime = System.nanoTime();
            CFSAnalyzer analyzer = new CFSAnalyzer(testConf.mappingInfo, targetLatency);
            analyzer.analyze(); // without parallel
            boolean proposed_schedulability = analyzer.checkSystemSchedulability();
            int proposed_timeConsumption = (int) ((System.nanoTime() - startTime) / 1000);
            // System.out.println("Time consumption (Analysis): " + proposed_timeConsumption + " us");

            // save analysis results into file
            AnalysisResultSaver analysisResultSaver = new AnalysisResultSaver();
            // (for testing purpose) if taskInfoPath is "tasks.json", then change taskInfoPath
            if (taskInfoPath.equals("tasks.json")) {
                taskInfoPath ="app/src/main/resources/generated_taskset/1cores_3tasks_0.5utilization_0.json";
            }
            analysisResultSaver.saveResultSummary(resultDir, taskInfoPath, simulator_schedulability, simulator_timeConsumption,
                    proposed_schedulability, proposed_timeConsumption);
            analysisResultSaver.saveDetailedResult(resultDir, taskInfoPath, testConf);
        }
    }

    private static boolean analyze_by_CFS_simulator(TestConfiguration testConf, ScheduleSimulationMethod scheduleMethod,
            ComparatorCase compareCase, long simulationTime, long schedule_try_count, String logger_option) throws ClassNotFoundException, NoSuchMethodException, SecurityException,
            InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        LoggerUtility.initializeLogger(logger_option);
        LoggerUtility.addConsoleLogger();

        // for brute-force method, unordered comparator is used.
        if ((scheduleMethod == ScheduleSimulationMethod.BRUTE_FORCE || scheduleMethod == ScheduleSimulationMethod.RANDOM) && 
            (compareCase != ComparatorCase.RELEASE_TIME && compareCase != ComparatorCase.FIFO)) {
            compareCase = ComparatorCase.FIFO;
        }

        CFSSimulator CFSSimulator = new CFSSimulator(scheduleMethod, compareCase, targetLatency, schedule_try_count);
        Logger logger = LoggerUtility.getLogger();
        boolean system_schedulability = true;

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
                system_schedulability = simulResult.schedulability;
            }
        } else {
            SimulationResult simulResult = CFSSimulator.simulateCFS(testConf.mappingInfo, -1, simulationTime);
            system_schedulability = simulResult.schedulability;
            for (Integer taskID : testConf.idNameMap.keySet()) {
                long WCRT_by_simulator = (simulResult.wcrtMap.get(taskID)/1000);
                long deadline = CFSSimulator.findTaskbyID(testConf, taskID.intValue()).period/1000;
                boolean task_schedulability = (WCRT_by_simulator <= deadline);
                CFSSimulator.findTaskbyID(testConf, taskID.intValue()).isSchedulable_by_simulator = task_schedulability;
                CFSSimulator.findTaskbyID(testConf, taskID.intValue()).WCRT_by_simulator = (int) WCRT_by_simulator;
                logger.info(String.format("Task ID with %3d (WCRT: %8d us, Period: %8d us, Schedulability: %5s)", taskID, WCRT_by_simulator, deadline, task_schedulability));
            }
            logger.info("Schedule execution count: " + CFSSimulator.getTriedScheduleCount());
            if (system_schedulability) {
                logger.info("All tasks are schedulable");
            } else {
                logger.info("Not all tasks are schedulable");
            }
        }

        return system_schedulability;
    }

    // (future work)for shared contention
    // private static void analyze_all_combinations_by_CFS_simulator(TestConfiguration testConf,
    //         ScheduleSimulationMethod scheduleMethod, ComparatorCase compareCase) {
    //     LoggerUtility.initializeLogger();
    //     LoggerUtility.addConsoleLogger();

    //     CFSSimulator CFSSimulator = new CFSSimulator(scheduleMethod, compareCase);

    //     long startTime = System.nanoTime();
    //     boolean schedulability = true;
    //     List<List<Core>> possibleCores = CombinationUtility.generatePossibleCores(testConf.mappingInfo);
    //     ExecutorService threadsForSimulation = Executors.newFixedThreadPool(maxNumThreads);
    //     List<Future<SimulationResult>> results = new ArrayList<>();

    //     for (List<Core> possibleCore : possibleCores) {
    //         for (Integer taskID : testConf.idNameMap.keySet()) {
    //             Future<SimulationResult> futureResult = threadsForSimulation
    //                     .submit(() -> CFSSimulator.simulateCFS(possibleCore, taskID.intValue()));
    //             results.add(futureResult);
    //         }

    //     }
    //     for (Future<SimulationResult> future : results) {
    //         try {
    //             SimulationResult simulationResult = future.get();
    //             if (!simulationResult.schedulability) {
    //                 System.out.println("Not all tasks are schedulable");
    //                 schedulability = false;
    //                 break;
    //             }
    //         } catch (Exception e) {
    //             e.printStackTrace();
    //         }
    //     }
    //     if (schedulability)
    //         System.out.println("All tasks are schedulable");

    //     threadsForSimulation.shutdown();

    //     long duration = (System.nanoTime() - startTime) / 1000;
    //     System.out.println("Time consumption (CFS simulator): " + duration + " ms");
    // }
}