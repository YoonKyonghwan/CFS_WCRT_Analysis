package org.cap;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CFSSimulatorTest {
    private CFSSimulator simulator;

    @Before
    public void setUp() {
        simulator = new CFSSimulator();
    }

    @Test
    public void testSingleTask() {
        // Define test tasks
        List<Task> tasks = Arrays.asList(
            new Task(1, 0, 1, 1, 1, 0, 10) // a single task
        );

        // Execute the method
        ArrayList<Double> WCRT = simulator.simulateCFS(tasks);

        // Make assertions about the expected result
        List<Double> expectedResult = Arrays.asList(
            3.0
        );
        assertEquals(expectedResult, WCRT);
    }

    @Test
    public void testMultipleTasks() {
        // Define test tasks
        List<Task> tasks = Arrays.asList(
            new Task(1, 0, 1, 2, 1, 0, 10), // multiple tasks
            new Task(2, 0, 1, 2, 1, 0, 10)
        );

        // Execute the method
        ArrayList<Double> WCRT = simulator.simulateCFS(tasks);

        // Make assertions about the expected result
        List<Double> expectedResult = Arrays.asList(
            8.0,
            8.0
        );
        assertEquals(expectedResult, WCRT);
    }

    @Test
    public void testReadAndWriteReleasedTogether() {
        // Define test tasks
        List<Task> tasks = Arrays.asList(
                new Task(1, 0, 1, 2, 1, 0, 10), // write is released at t=3
                new Task(2, 3, 1, 2, 1, 0, 10)  // read is released at t=3
        );

        // Execute the method
        ArrayList<Double> WCRT = simulator.simulateCFS(tasks);

        // Make assertions about the expected result
        List<Double> expectedResult = Arrays.asList(
                6.0,
                5.0
        );
        assertEquals(expectedResult, WCRT);
    }

    @Test
    public void testReadReleasedWhenWriteIsExecuting() {
        // Define test tasks
        List<Task> tasks = Arrays.asList(
                new Task(1, 0, 1, 1, 2, 0, 10), // write is executing at t=3
                new Task(2, 3, 1, 1, 2, 0, 10)  // read is released at t=3
        );

        // Execute the method
        ArrayList<Double> WCRT = simulator.simulateCFS(tasks);

        // Make assertions about the expected result
        List<Double> expectedResult = Arrays.asList(
                4.0,
                5.0
        );
        assertEquals(expectedResult, WCRT);
    }

    @Test
    public void testTasksWithDifferentPeriods() {
        // Define test tasks
        List<Task> tasks = Arrays.asList(
            new Task(1, 0, 1.0, 1.0, 1.0, 0, 15),  // period 15
            new Task(2, 0, 1.0, 1.0, 1.0, 0, 5)   // period 5
        );

        // Execute the method
        ArrayList<Double> WCRT = simulator.simulateCFS(tasks);

        // Make assertions about the expected result
        List<Double> expectedResult = Arrays.asList(
            8.0,
            8.0
        );
        assertEquals(expectedResult, WCRT);
    }

    @Test
    public void testTasksWithDifferentNice() {
        // Define test tasks
        List<Task> tasks = Arrays.asList(
            new Task(1, 0, 1.0, 2.0, 1.0, 0, 15),  // nice value 0
            new Task(2, 0, 1.0, 2.0, 1.0, 10, 15)  // nice value 10
        );

        // Execute the method
        ArrayList<Double> WCRT = simulator.simulateCFS(tasks);

        // Make assertions about the expected result
        List<Double> expectedResult = Arrays.asList(
            8.0,
            11.0
        );
        assertEquals(expectedResult, WCRT);
    }

    // TODO read, write orders different due to rounding policy
}
