package org.cap.simulation;

import org.cap.model.Core;
import org.cap.model.SimulationResult;
import org.cap.utility.LoggerUtility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class CFSSimulator {
    private static final Logger logger = LoggerUtility.getLogger();

    // TODO move to separate file
    private static final List<Integer> priorityToWeight = Arrays.asList(
            88761, 71755, 56483, 46273, 36291,
            29154, 23254, 18705, 14949, 11916,
            9548, 7620, 6100, 4904, 3906,
            3121, 2501, 1991, 1586, 1277,
            1024, 820, 655, 526, 423,
            335, 272, 215, 172, 137,
            110, 87, 70, 56, 45,
            36, 29, 23, 18, 15
    );

    public SimulationResult simulateCFS(List<Core> cores) {
        LoggerUtility.initializeLogger();
        logger.info("Starting CFS simulation");

        boolean schedulability = true;
        List<List<Double>> WCRTs = initializeWCRTs(cores);
        // TODO CFS simulation

        LoggerUtility.addConsoleLogger();
        return new SimulationResult(schedulability, WCRTs);
    }

    private static List<List<Double>> initializeWCRTs(List<Core> cores) {
        List<List<Double>> WCRTs = new ArrayList<>();
        for (Core core: cores) {
            WCRTs.add(new ArrayList<>(Collections.nCopies(core.tasks.size(), 0.0)));
        }
        return WCRTs;
    }

}
