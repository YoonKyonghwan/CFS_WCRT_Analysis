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
