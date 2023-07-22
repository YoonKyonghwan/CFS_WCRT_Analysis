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
    public void testSimulateCFS() {
        // Define test tasks
        List<Task> tasks = Arrays.asList(
            new Task(1, 0, 1, 2, 1, 0, 10),
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
}
