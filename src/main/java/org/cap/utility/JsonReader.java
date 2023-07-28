package org.cap.utility;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.cap.model.Core;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

public class JsonReader {

    public List<Core> readTasksFromFile(String filename) {
        Gson gson = new Gson();
        Type taskListType = new TypeToken<List<Core>>(){}.getType();

        try (FileReader reader = new FileReader(filename)) {
            return gson.fromJson(reader, taskListType);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read tasks from file", e);
        }
    }
}