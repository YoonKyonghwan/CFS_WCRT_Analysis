package org.cap.utility;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.sourceforge.argparse4j.inf.Namespace;

import org.cap.model.Core;
import org.cap.model.Task;
import org.cap.model.TestConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonTaskCreator {

    public void run(Namespace params){
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
        double utilization = params.getDouble("utilization");
        String generatedFilesSaveDir = params.getString("generated_files_save_dir");
        generateFile(numSets, numTasks, numCores, utilization, generatedFilesSaveDir);
    }

    public void generateFile(int numTasksets, int numTasks, int numCores, double utilization, String generatedFilesSaveDir) {
        for (int tasksetIndex=0; tasksetIndex<numTasksets; tasksetIndex++){
            List<Core> cores = new ArrayList<>();
            for (int i = 1; i <= numCores; i++) {
                Core core = new Core(i, new ArrayList<>());
                cores.add(core);
            }

            TestConfiguration testConf = generateTasks(numTasks, utilization, cores); //set tasks_info into Core
            saveToFile(testConf, numCores, numTasks, utilization, tasksetIndex, generatedFilesSaveDir);
        }
        return;
    }

    private TestConfiguration generateTasks(int numTasks, double utilization, List<Core> cores) {
        int coreIndex = 0;
        double remainingUtilization = utilization;

        for (int i = 1; i <= numTasks; i++) {
            Task task = new Task();
            task.id = i;
            task.startTime = 0; // task.readTime = generateBlockingTime();
            task.readTime = 0;
            task.bodyTime = Math.round(Math.random() * 100);    //randomly sampled from [0, 100]
            task.writeTime = 0;  // task.writeTime = generateBlockingTime();
            task.nice = (int) Math.round(Math.random() * 19);   //randomly sampled from [0, 19]
            task.index = cores.get(coreIndex).tasks.size();
            remainingUtilization = setPeriod(numTasks, remainingUtilization, i, task);
            
            cores.get(coreIndex).tasks.add(task);
            coreIndex = (coreIndex + 1) % cores.size();
        }

        TestConfiguration testConf = new TestConfiguration();
        testConf.mappingInfo = cores;
        HashMap<Integer, String> idNameMap = new HashMap<Integer, String>();
        for (int i = 1; i <= numTasks; i++){
            String task_name = "task" + i;
            idNameMap.put(i, task_name);
        }
        testConf.idNameMap = idNameMap;

        return testConf;
    }

    private double setPeriod(int numTasks, double remainingUtilization, int i, Task task) {
        double totalExecution = task.readTime + task.bodyTime + task.writeTime;
        double taskUtilization = 0;
        if (i == numTasks) {
            taskUtilization = remainingUtilization;
        } else {
            //taskUtilization is randomly sampled from [0.1, 2/3*remainingUtilization]
            taskUtilization = Math.random() * ((2.0 * remainingUtilization) / 3.0);
            if (taskUtilization < 0.1) taskUtilization = 0.1;
        }
        int period = (int) Math.ceil(totalExecution / taskUtilization);
        // period = (int) Math.ceil(period / 10.0) * 10; // round up to nearest 10
        
        task.period = period;
        remainingUtilization -= taskUtilization;
        return remainingUtilization;
    }
    

    private double generateSharedResourceAccessTime() {
        double chance = Math.random();

        if (chance < 0.2) {
            return 0;
        } else {
            return 50.0 + Math.round(Math.random() * 10) * 5.0;
        }
    }

    private void saveToFile(TestConfiguration testConf, int numCores, int numTasks, double utilization, int tasksetIndex, String generatedFilesSaveDir) {



        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();
        String tasks_info = gson.toJson(testConf);
        
        // get project base directory
        Path saveDir = Paths.get(generatedFilesSaveDir).toAbsolutePath();
        // If directory is not existed, create it
        if (!Files.exists(saveDir)) {
            try {
                Files.createDirectories(saveDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Directory to save : " + saveDir.toString());
        
        String fileName = numCores + "cores_" + numTasks + "tasks_" + utilization + "utilization" + "_" + tasksetIndex + ".json";
        Path filePath = Paths.get(saveDir.toString(), fileName);
        try {
            Files.write(filePath, tasks_info.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return;
    }
}
