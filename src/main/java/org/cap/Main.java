package org.cap;

import org.cap.model.Core;
import org.cap.model.SimulationResult;
import org.cap.model.Task;
import org.cap.simulation.Analyzer;
import org.cap.simulation.CFSSimulator;
import org.cap.simulation.PFSSimulator;
import org.cap.utility.JsonReader;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        JsonReader jsonReader = new JsonReader();
        List<Core> cores = jsonReader.readTasksFromFile("tasks.json");
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
        CFSSimulator CFSSimulator = new CFSSimulator();

        long startTime = System.currentTimeMillis();

        List<List<Core>> possibleCores = generatePossibleCores(cores);

        for (List<Core> possibleCore : possibleCores) {
            SimulationResult simulationResult = CFSSimulator.simulateCFS(possibleCore);

            if (simulationResult.schedulability) {
                System.out.println("All tasks are schedulable");
            } else {
                System.out.println("Not all tasks are schedulable");
                break;
            }
        }

        long duration = (System.currentTimeMillis() - startTime);
        System.out.println("Time consumption (CFS simulator): " + duration + " ms");
    }
    public static List<List<Core>> generatePossibleCores(List<Core> cores) {
        List<List<Core>> result = new ArrayList<>();
        generateCombinations(cores, 0, 0, result, deepCopyCoreList(cores));
        return result;
    }

    public static void generateCombinations(List<Core> cores, int coreIndex, int taskIndex, List<List<Core>> result, List<Core> currentCores) {
        if (coreIndex == cores.size()) {
            result.add(deepCopyCoreList(currentCores));
            return;
        }

        if (taskIndex == cores.get(coreIndex).tasks.size()) {
            generateCombinations(cores, coreIndex + 1, 0, result, currentCores);
            return;
        }

        for (int j = 0; j < cores.get(coreIndex).tasks.get(taskIndex).period; j++) {
            currentCores.get(coreIndex).tasks.get(taskIndex).startTime = j;
            generateCombinations(cores, coreIndex, taskIndex + 1, result, currentCores);
        }
    }

    public static List<Core> deepCopyCoreList(List<Core> cores) {
        List<Core> newList = new ArrayList<>();
        for (Core core : cores) {
            newList.add(core.copy());
        }
        return newList;
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