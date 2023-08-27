package org.cap.utility;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.cap.model.Core;
import org.cap.model.Task;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class JsonTaskCreator {

    private static Random rand = new Random();

    public static String generateFile(int numberOfTasks, double cpuUtilization) {
        List<Core> cores = new ArrayList<>();
        List<Task> tasks = new ArrayList<>();

        generateTasks(numberOfTasks, cpuUtilization, tasks);

        for (int i = 1; i <= 2; i++) {
            Core core = new Core(i, new ArrayList<>(tasks));
            cores.add(core);
        }

        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();
        String json = gson.toJson(cores);

        String filename = "2cores_" + numberOfTasks + "tasks.json";
        try (FileWriter file = new FileWriter(filename)) {
            file.write(json);
            file.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return filename;
    }

    private static void generateTasks(int numberOfTasks, double cpuUtilization, List<Task> tasks) {
        double totalTime = 0;

        // Generate tasks and calculate total time
        for (int i = 1; i <= numberOfTasks; i++) {
            Task task = new Task();
            task.id = i;
            task.startTime = 0;
            task.readTime = generateBlockingTime();
            task.bodyTime = 500.0 + Math.round(rand.nextDouble() * 100) * 5.0;
            task.writeTime = generateBlockingTime();
            task.nice = 0;
            // TODO fix index; should be initialized based on core
            task.index = i - 1;

            totalTime += task.readTime + task.bodyTime + task.writeTime;

            tasks.add(task);
        }

        int period = (int) Math.ceil(totalTime / cpuUtilization);

        for (Task task : tasks) {
            task.period = period;
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
}
