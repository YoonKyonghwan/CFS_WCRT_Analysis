package org.cap.utility;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.cap.model.Core;
import org.cap.model.Task;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class JsonTaskCreator {

    public void generateFile(int numTasksets, int numTasks, int numCores, double utilization, String generatedFilesSaveDir) {
        List<Core> cores = new ArrayList<>();
        for (int i = 0; i < numCores; i++) {
            Core core = new Core(i, new ArrayList<>());
            cores.add(core);
        }

        for (int tasksetIndex=0; tasksetIndex<numTasksets; tasksetIndex++){
            generateTasks(numTasks, utilization, cores); //set tasks_info into Core
            saveToFile(cores, numTasks, utilization, tasksetIndex, generatedFilesSaveDir);
        }
        return;
    }

    private static void generateTasks(int numberOfTasks, double cpuUtilization, List<Core> cores) {
        double totalTime = 0;
        int coreIndex = 0;

        for (int i = 1; i <= numberOfTasks; i++) {
            Task task = new Task();
            task.id = i;
            task.startTime = 0; // task.readTime = generateBlockingTime();
            task.readTime = 0;
            task.bodyTime = Math.round(Math.random() * 100);
            task.writeTime = 0;  // task.writeTime = generateBlockingTime();
            task.nice = 0;
            task.index = cores.get(coreIndex).tasks.size();

            totalTime += task.readTime + task.bodyTime + task.writeTime;
            cores.get(coreIndex).tasks.add(task);
            coreIndex = (coreIndex + 1) % cores.size();
        }

        int period = (int) Math.ceil(totalTime / cpuUtilization);
        // round up to nearest 10
        period = (int) Math.ceil(period / 10.0) * 10;

        for (Core core : cores) {
            for (Task task : core.tasks) {
                task.period = period;
            }
        }
        return;
    }

    private double generateSharedResourceAccessTime() {
        double chance = Math.random();

        if (chance < 0.2) {
            return 0;
        } else {
            return 50.0 + Math.round(Math.random() * 10) * 5.0;
        }
    }

    private void saveToFile(List<Core> cores, int numTasks, double utilization, int tasksetIndex, String generatedFilesSaveDir) {
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();
        String tasks_info = gson.toJson(cores);

        int numCores = cores.size();
        
        Path saveDir = Paths.get(generatedFilesSaveDir);
        if (!Files.exists(saveDir)) {
            try {
                Files.createDirectory(saveDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        String fileName = numCores + "cores_" + numTasks + "tasks_" + utilization + "utilization" + "_" + tasksetIndex + ".json";
        Path filePath = Paths.get(generatedFilesSaveDir, fileName);
        try {
            Files.write(filePath, tasks_info.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return;
    }
}
