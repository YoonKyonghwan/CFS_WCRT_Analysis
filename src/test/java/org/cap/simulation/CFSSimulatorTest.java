package org.cap.simulation;

import org.cap.model.Core;
import org.cap.model.Task;
import org.cap.utility.LoggerUtility;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.List;

public class CFSSimulatorTest {
    private CFSSimulator simulator;

    @Before
    public void setUp() {
        LoggerUtility.initializeLogger();
        LoggerUtility.addConsoleLogger();
        simulator = new CFSSimulator();
    }

    @Test
    public void testOneTask() {
        // Define test tasks
        List<Task> tasksInFirstCore = List.of(
            new Task(1, 0, 1, 1, 1, 0, 10, 0) // a single task
        );
        List<Core> cores = List.of(
            new Core(1, tasksInFirstCore) // a single task
        );

        // Execute the method
        List<List<Double>> WCRTs = simulator.simulateCFS(cores).WCRTs;

        // Make assertions about the expected result
        List<List<Double>> expectedResult = List.of(
            List.of(
            3.0
            )
        );
        assertEquals(expectedResult, WCRTs);
    }

    @Test
    public void testTwoTasks() {
        // Define test tasks
        List<Task> tasksInFirstCore = List.of(
            new Task(1, 0, 1, 1, 1, 0, 10, 0),
            new Task(2, 0, 1, 1, 1, 0, 10, 1)
        );
        List<Core> cores = List.of(
                new Core(1, tasksInFirstCore)
        );

        // Execute the method
        List<List<Double>> WCRTs = simulator.simulateCFS(cores).WCRTs;

        // Make assertions about the expected result
        List<List<Double>> expectedResult = List.of(
            List.of(
                6.0,
                6.0
            )
        );
        assertEquals(expectedResult, WCRTs);
    }

    @Test
    public void testRuntimeSmallerThanMinimumGranularity() {
        // Define test tasks
        List<Task> tasksInFirstCore = List.of(
                new Task(1, 0, 0, 5, 0, 0, 100, 0),
                new Task(2, 0, 0, 5, 0, 0, 100, 1),
                new Task(3, 0, 0, 5, 0, 0, 100, 2),
                new Task(4, 0, 0, 5, 0, 0, 100, 3),
                new Task(5, 0, 0, 5, 0, 0, 100, 4),
                new Task(6, 0, 0, 5, 0, 0, 100, 5),
                new Task(7, 0, 0, 5, 0, 0, 100, 6),
                new Task(8, 0, 0, 5, 0, 0, 100, 7),
                new Task(9, 0, 0, 5, 0, 0, 100, 8),
                new Task(10, 0, 0, 5, 0, 0, 100, 9)
        );
        List<Core> cores = List.of(
                new Core(1, tasksInFirstCore)
        );

        // Execute the method
        List<List<Double>> WCRTs = simulator.simulateCFS(cores).WCRTs;

        // Make assertions about the expected result
        List<List<Double>> expectedResult = List.of(
            List.of(
                50.0,
                50.0,
                50.0,
                50.0,
                50.0,
                50.0,
                50.0,
                50.0,
                50.0,
                50.0
            )
        );
        assertEquals(expectedResult, WCRTs);
    }

    @Test
    public void testOneReadAndOneBodyTask() {
        // Define test tasks
        List<Task> tasksInFirstCore = List.of(
            new Task(1, 0, 10, 10, 0, 0, 100, 0),
            new Task(2, 0, 0, 20, 0, 0, 100, 1)
        );
        List<Core> cores = List.of(
            new Core(1, tasksInFirstCore)
        );

        // Execute the method
        List<List<Double>> WCRTs = simulator.simulateCFS(cores).WCRTs;

        // Make assertions about the expected result
        List<List<Double>> expectedResult = List.of(
            List.of(
                40.0,
                40.0
            )
        );
        assertEquals(expectedResult, WCRTs);
    }

    @Test
    public void testOneWriteAndOneBodyTask() {
        // Define test tasks
        List<Task> tasksInFirstCore = List.of(
            new Task(1, 0, 0, 10, 10, 0, 100, 0),
            new Task(2, 0, 0, 20, 0, 0, 100, 1)
        );
        List<Core> cores = List.of(
            new Core(1, tasksInFirstCore)
        );

        // Execute the method
        List<List<Double>> WCRTs = simulator.simulateCFS(cores).WCRTs;

        // Make assertions about the expected result
        List<List<Double>> expectedResult = List.of(
            List.of(
                    40.0,
                    40.0
            )
        );
        assertEquals(expectedResult, WCRTs);
    }

    @Test
    public void testReadTasksInTwoCores() {
        // Define test tasks
        List<Task> tasksInFirstCore = List.of(
            new Task(1, 0, 10, 10, 0, 0, 100, 0)
        );
        List<Task> tasksInSecondCore = List.of(
            new Task(2, 0, 10, 10, 0, 0, 100, 0)
        );
        List<Core> cores = List.of(
            new Core(1, tasksInFirstCore),
            new Core(2, tasksInSecondCore)
        );

        // Execute the method
        List<List<Double>> WCRTs = simulator.simulateCFS(cores).WCRTs;

        // Make assertions about the expected result
        List<List<Double>> expectedResult = List.of(
            List.of(
                30.0
            ),
            List.of(
                30.0
            )
        );
        assertEquals(expectedResult, WCRTs);
    }

}