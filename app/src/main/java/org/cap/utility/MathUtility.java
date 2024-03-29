package org.cap.utility;

import org.cap.model.Core;
import org.cap.model.Task;

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

    
    public static void assignNiceValues(List<Core> cores, double lambda) {
        for (Core core : cores) {
            // check min deadline
            long min_deadline = 10 * 1000 * 1000; // with the initial large value(10 second)
            for (Task task : core.tasks) {
                if (task.period < min_deadline) {
                    min_deadline = task.period;
                }
            }
            // assign nice values
            for (Task task : core.tasks) {
                task.nice = computeNice(task.period, min_deadline, lambda);
            }
        }
    }

    //nice_i = \min \left(-20 +  \ceil*{\log_{1.25} \frac{D_i}{D_{\text{min}}}}, \;19 \right)
    private static int computeNice(long deadline_i, long min_deadline, double lambda){
        double relative_weight = Math.log(deadline_i / min_deadline) / Math.log(1.25);
        relative_weight *= lambda;
        int ret = (int) Math.min(-20 + Math.ceil(relative_weight), 19);
        return ret;
    }
}
