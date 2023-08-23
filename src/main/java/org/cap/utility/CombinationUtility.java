package org.cap.utility;

import org.cap.model.Core;

import java.util.ArrayList;
import java.util.List;

public class CombinationUtility {

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

}
