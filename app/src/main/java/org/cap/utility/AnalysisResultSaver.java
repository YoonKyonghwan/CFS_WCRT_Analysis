package org.cap.utility;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnalysisResultSaver {
    public void saveResultSummary(String resultDir, String taskInfoPath, boolean simulator_schedulability,
            int simulator_timeConsumption,
            boolean proposed_schedulability, int proposed_timeConsumption) {

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

        // Prepare data to be written to the CSV file
        String dataToWrite = String.format("%s,%s,%s,%s,%s,%d,%s,%d\n",
                numCores, numTasks, utilization, tasksetIndex,
                simulator_schedulability, simulator_timeConsumption,
                proposed_schedulability, proposed_timeConsumption);

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
                writer.write("numCores,numTasks,utilization,tasksetIndex,simulator_schedulability,simulator_timeConsumption,proposed_schedulability,proposed_timeConsumption\n");
            }

            writer.write(dataToWrite);
            System.out.println("Results saved to " + resultFileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
