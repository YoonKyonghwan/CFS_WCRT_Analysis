package org.cap.utility;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.cap.model.TestConfiguration;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;

public class JsonReader {

    public TestConfiguration readTasksFromFile(String filename) {
        Gson gson = new Gson();
        Type taskListType = new TypeToken<TestConfiguration>(){}.getType();

        try (FileReader reader = new FileReader(filename)) {
            return gson.fromJson(reader, taskListType);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read tasks from file", e);
        }
    }
}