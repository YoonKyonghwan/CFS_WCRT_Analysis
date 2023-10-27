package org.cap.utility;

import org.cap.model.Core;

import java.util.List;

public class MathUtility {

    public static boolean withinTolerance(double a, double b) {
        double tolerance = 1E-10;
        return (a - b) < tolerance;
    }

    public static long getLCM(List<Core> cores) {
        return cores.stream()
                .flatMap(core -> core.tasks.stream())
                .map(task -> task.period)
                .reduce(1L, (a, b) -> a * (b / getGCD(a, b)));
    }

    public static long getGCD(long a, long b) {
        if (b == 0) {
            return a;
        } else {
            return getGCD(b, a % b);
        }
    }
}
