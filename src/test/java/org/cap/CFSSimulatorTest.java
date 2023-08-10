package org.cap;

import org.cap.model.Core;
import org.cap.model.Task;
import org.cap.simulation.CFSSimulator;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.List;

public class CFSSimulatorTest {
    private CFSSimulator simulator;

    @Before
    public void setUp() {
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
            new Task(1, 0, 1, 2, 1, 0, 10, 0), // two tasks
            new Task(2, 0, 1, 2, 1, 0, 10, 1)
        );
        List<Core> cores = List.of(
            new Core(1, tasksInFirstCore) // a single task
        );

        // Execute the method
        List<List<Double>> WCRTs = simulator.simulateCFS(cores).WCRTs;

        // Make assertions about the expected result
        List<List<Double>> expectedResult = List.of(
            List.of(
                8.0,
                8.0
            )
        );
        assertEquals(expectedResult, WCRTs);
    }

    @Test
    public void testReadAndWriteReleasedTogether() {
        // Define test tasks
        List<Task> tasksInFirstCore = List.of(
            new Task(1, 0, 1, 2, 1, 0, 10, 0), // write is released at t=3
            new Task(2, 3, 1, 2, 1, 0, 10, 1)  // read is released at t=3
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
                5.0
            )
        );
        assertEquals(expectedResult, WCRTs);
    }

    @Test
    public void testReadReleasedWhenWriteIsExecuting() {
        // Define test tasks
        List<Task> tasksInFirstCore = List.of(
            new Task(1, 0, 1, 1, 2, 0, 10, 0), // write is executing at t=3
            new Task(2, 3, 1, 1, 2, 0, 10, 1)  // read is released at t=3
        );
        List<Core> cores = List.of(
            new Core(1, tasksInFirstCore)
        );

        // Execute the method
        List<List<Double>> WCRTs = simulator.simulateCFS(cores).WCRTs;

        // Make assertions about the expected result
        List<List<Double>> expectedResult = List.of(
            List.of(
                4.0,
                5.0
            )
        );
        assertEquals(expectedResult, WCRTs);
    }

    @Test
    public void testTasksWithDifferentPeriods() {
        // Define test tasks
        List<Task> tasksInFirstCore = List.of(
            new Task(1, 0, 1.0, 1.0, 1.0, 0, 15, 0),  // period 15
            new Task(2, 0, 1.0, 1.0, 1.0, 0, 5, 1)   // period 5
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
    public void testTasksWithDifferentNice() {
        // Define test tasks
        List<Task> tasksInFirstCore = List.of(
            new Task(1, 0, 1.0, 2.0, 1.0, 0, 15, 0),  // nice value 0
            new Task(2, 0, 1.0, 2.0, 1.0, 10, 15, 1)  // nice value 10
        );
        List<Core> cores = List.of(
            new Core(1, tasksInFirstCore)
        );

        // Execute the method
        List<List<Double>> WCRTs = simulator.simulateCFS(cores).WCRTs;

        // Make assertions about the expected result
        List<List<Double>> expectedResult = List.of(
            List.of(
                8.0,
                11.0
            )
        );
        assertEquals(expectedResult, WCRTs);
    }

    @Test
    public void testRounding() {
        // Define test tasks
        List<Task> tasksInFirstCore = List.of(
            new Task(1, 0, 0, 1, 0, 0, 10, 0), // three tasks
            new Task(2, 0, 0, 1, 0, 0, 10, 1),
            new Task(3, 0, 0, 1, 0, 0, 10, 2)
        );
        List<Core> cores = List.of(
            new Core(1, tasksInFirstCore)
        );

        // Execute the method
        List<List<Double>> WCRTs = simulator.simulateCFS(cores).WCRTs;

        // Make assertions about the expected result
        List<List<Double>> expectedResult = List.of(
            List.of(
                3.0,
                3.0,
                3.0
            )
        );
        assertEquals(expectedResult, WCRTs);
    }

    @Test
    public void testFIFOWithReadAndWrite() {
        // Define test tasks
        List<Task> tasksInFirstCore = List.of(
            new Task(1, 0, 5, 1, 0, 0, 10, 0), // three tasks
            new Task(2, 0, 0, 1, 1, 0, 10, 1),
            new Task(3, 4, 0, 1, 1, 0, 10, 2)
        );
        List<Core> cores = List.of(
            new Core(1, tasksInFirstCore)
        );

        // Execute the method
        List<List<Double>> WCRTs = simulator.simulateCFS(cores).WCRTs;

        // Make assertions about the expected result
        List<List<Double>> expectedResult = List.of(
            List.of(
                9.0,
                9.0,
                6.0
            )
        );
        assertEquals(expectedResult, WCRTs);
    }

    @Test
    public void testFIFOWithWrite() {
        // Define test tasks
        List<Task> tasksInFirstCore = List.of(
            new Task(1, 0, 0, 1, 5, 0, 10, 0), // three tasks
            new Task(2, 1, 0, 1, 1, 0, 10, 1),
            new Task(3, 4, 0, 1, 1, 0, 10, 2)
        );
        List<Core> cores = List.of(
                new Core(1, tasksInFirstCore)
        );

        // Execute the method
        List<List<Double>> WCRTs = simulator.simulateCFS(cores).WCRTs;

        // Make assertions about the expected result
        List<List<Double>> expectedResult = List.of(
            List.of(
                8.0,
                8.0,
                6.0
            )
        );
        assertEquals(expectedResult, WCRTs);
    }

    // TODO make more test cases
    // TODO check if path diverges at the start of simulation

}
