package org.cap.utility;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cap.model.Core;
import org.cap.model.Task;
import org.cap.model.TestConfiguration;

import com.google.gson.GsonBuilder;

public class AnalysisResultSaver {
    public void saveResultSummary(String resultDir, String taskInfoPath, boolean simulator_schedulability,
            long simulator_timeConsumption, boolean proposed_schedulability, long proposed_timeConsumption) {

        // Parse information from the filename
        String inputFileName = new File(taskInfoPath).getName();
        Pattern pattern = Pattern.compile("(\\d+)cores_(\\d+)tasks_(\\d+(?:\\.\\d+)?)utilization_(\\d+)\\.json");
        Matcher matcher = pattern.matcher(inputFileName);

        if (!matcher.matches()) {
            System.err.println("Invalid input file name format");
            return;
        }

        String numCores = matcher.group(1);
        String numTasks = matcher.group(2);
        String utilization = matcher.group(3);
        String tasksetIndex = matcher.group(4);

        // Create the result directory if it doesn't exist
        File dir = new File(resultDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // Save the data into the CSV file
        String resultFileName = resultDir + "/result_summary.csv";
        File file = new File(resultFileName);
        
        boolean shouldWriteHeaders = !file.exists();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(resultFileName, true))) {
            if (shouldWriteHeaders) {
                // If the file didn't exist before, write the headers first
                String header = "numCores,numTasks,utilization," + 
                "tasksetIndex,simulator_schedulability," +
                "simulator_timeConsumption(us),proposed_schedulability,proposed_timeConsumption(us)\n";
                writer.write(header);
            }

            // Prepare data to be written to the CSV file
            String dataToWrite = String.format("%s,%s,%s,%s,%s,%d,%s,%d\n",
                    numCores, numTasks, utilization, tasksetIndex,
                    simulator_schedulability, simulator_timeConsumption,
                    proposed_schedulability, proposed_timeConsumption);
            writer.write(dataToWrite);
            // System.out.println("Results saved to " + resultFileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveDetailedResult(String resultDir, String taskInfoPath, TestConfiguration testConf){
        String inputFileName = new File(taskInfoPath).getName();
        Pattern pattern = Pattern.compile("(\\d+)cores_(\\d+)tasks_(\\d+(?:\\.\\d+)?)utilization_(\\d+)\\.json");
        Matcher matcher = pattern.matcher(inputFileName);
        
        if (!matcher.matches()) {
            System.err.println("Invalid input file name format");
            return;
        }
        
        String numCores = matcher.group(1);
        String numTasks = matcher.group(2);
        String utilization = matcher.group(3);
        
        // Create the result directory if it doesn't exist
        String detailResultDir = resultDir + "/detail_result/" + numCores + "cores/" + numTasks + "tasks/" + utilization + "utilization";
        File dir = new File(detailResultDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String resultFileName = detailResultDir + "/" + inputFileName.replace(".json", "_result.csv");
        File file = new File(resultFileName);
        if (file.exists()) {
            file.delete();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(resultFileName, true))) {
            // write the header first
            String header = "id,deadline,WCET,nice,name," +
            "WCRT_by_simulator,WCRT_by_proposed," +
            "simulator_schedulability,proposed_schedulability\n";
            writer.write(header);

            // write the result for each task
            String dataToWrite;
            for (Core core : testConf.mappingInfo){
                for (Task task : core.tasks){
                    int id = task.id;
                    int deadline = (int) task.period;
                    String name = testConf.idNameMap.get(task.id);
                    int WCRT_by_simulator = (int) task.WCRT_by_simulator;
                    boolean isSchedulable_by_simulator = task.isSchedulable_by_simulator;
                    int WCRT_by_proposed = (int) task.WCRT_by_proposed;
                    int WCET = (int) task.bodyTime;
                    int nice = task.nice;
                    boolean isSchedulable_by_proposed = task.isSchedulable_by_proposed;

                    // Prepare data to be written to the CSV file
                    dataToWrite = String.format("%d,%d,%d,%d,%s,%d,%d,%b,%b\n",
                            id, deadline, WCET, nice, name, WCRT_by_simulator, WCRT_by_proposed, 
                            isSchedulable_by_simulator, isSchedulable_by_proposed);
                    
                    writer.write(dataToWrite);
                }
            }
            // System.out.println("Detailed results saved to " + resultFileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateNiceValues(String taskInfoPath, TestConfiguration testConf){
        // save the updated nice values to the file
        GsonBuilder gsonbuilder = new GsonBuilder();
        String updatedTaskInfo = gsonbuilder.excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create().toJson(testConf);
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(taskInfoPath))) {
            writer.write(updatedTaskInfo);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
