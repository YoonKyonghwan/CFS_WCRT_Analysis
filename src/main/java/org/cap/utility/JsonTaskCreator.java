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
import java.util.Random;

public class JsonTaskCreator {

    private static Random rand = new Random();

    public static String generateFile(int numberOfTasks, double cpuUtilization) {
        List<Core> cores = new ArrayList<>();

        for (int i = 1; i <= 2; i++) {
            Core core = new Core(i, new ArrayList<>());
            cores.add(core);
        }

        generateTasks(numberOfTasks, cpuUtilization, cores);

        return createFile(numberOfTasks, cpuUtilization, cores);
    }

    private static void generateTasks(int numberOfTasks, double cpuUtilization, List<Core> cores) {
        double totalTime = 0;
        int coreIndex = 0;

        for (int i = 1; i <= numberOfTasks; i++) {
            Task task = new Task();
            task.id = i;
            task.startTime = 0;
            task.readTime = generateBlockingTime();
            task.bodyTime = 500.0 + Math.round(Math.random() * 100) * 5.0;
            task.writeTime = generateBlockingTime();
            task.nice = 0;
            task.index = cores.get(coreIndex).tasks.size();

            totalTime += task.readTime + task.bodyTime + task.writeTime;

            cores.get(coreIndex).tasks.add(task);

            coreIndex = (coreIndex + 1) % cores.size();
        }

        int period = (int) Math.ceil(totalTime / cpuUtilization);

        for (Core core : cores) {
            for (Task task : core.tasks) {
                task.period = period;
            }
        }
    }

    private static double generateBlockingTime() {
        double chance = rand.nextDouble();

        if (chance < 0.2) {
            return 0;
        } else {
            return 50.0 + Math.round(rand.nextDouble() * 10) * 5.0;
        }
    }

    private static String createFile(int numberOfTasks, double cpuUtilization, List<Core> cores) {
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();
        String json = gson.toJson(cores);

        String baseFilename = "2cores_" + numberOfTasks + "tasks_" + cpuUtilization + "cpuUtilization";

        Path folderPath = Paths.get("inputs");
        if (!Files.exists(folderPath)) {
            try {
                Files.createDirectory(folderPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        int counter = 1;
        String filename = baseFilename + "_1.json";
        Path filePath = Paths.get(folderPath.toString(), filename);

        while (Files.exists(filePath)) {
            counter++;
            filename = baseFilename + "_" + counter + ".json";
            filePath = Paths.get(folderPath.toString(), filename);
        }

        try {
            Files.write(filePath, json.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return filePath.toString();
    }
}
