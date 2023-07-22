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
        List<Task> tasks = Arrays.asList(
                // Define your test tasks here...
        );

        // Execute the method to test
        ArrayList<Double> WCRT = simulator.simulateCFS(tasks);

        // Make assertions about the expected result
        // TODO: You'll need to add a method to your CFSSimulator class
        // that allows you to get the result, so you can compare it here
        List<Double> expectedResult = Arrays.asList(
                // Define your expected result here...
        );

        assertEquals(expectedResult, WCRT);
    }
}
