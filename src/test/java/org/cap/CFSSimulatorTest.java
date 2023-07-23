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
        CFSSimulator simulator = new CFSSimulator();
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

//    @Test
//    public void testTasksWithDifferentNiceValues() {
//        // Define test tasks
//        CFSSimulator simulator = new CFSSimulator();
//        List<Task> tasks = Arrays.asList(
//            new Task(1, 0, 1.0, 1.0, 1.0, 0, 1),  // nice value 0
//            new Task(2, 0, 1.0, 1.0, 1.0, 10, 1) // nice value 10
//        );
//
//        // Execute the method
//        ArrayList<Double> WCRT = simulator.simulateCFS(tasks);
//
//        // Make assertions about the expected result
//        List<Double> expectedResult = Arrays.asList(
//            8.0,
//            8.0
//        );
//        assertEquals(expectedResult, WCRT);
//    }

//    @Test
//    public void testTasksWithDifferentPeriods() {
//        // Define test tasks
//        CFSSimulator simulator = new CFSSimulator();
//        List<Task> tasks = Arrays.asList(
//            new Task(1, 0, 1.0, 1.0, 1.0, 0, 3),  // period 3
//            new Task(2, 0, 1.0, 1.0, 1.0, 0, 6),  // period 6
//            new Task(3, 0, 1.0, 1.0, 1.0, 0, 9)   // period 9
//        );
//
//        // Execute the method
//        ArrayList<Double> WCRT = simulator.simulateCFS(tasks);
//
//        // Make assertions about the expected result
//        List<Double> expectedResult = Arrays.asList(
//            8.0,
//            8.0,
//            8.0
//        );
//        assertEquals(expectedResult, WCRT);
//    }
}
