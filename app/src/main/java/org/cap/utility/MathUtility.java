package org.cap.utility;

import org.cap.model.Core;
import org.cap.model.NiceToWeight;
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

    
    public static void assignNiceValues2(List<Core> cores, double lambda) {
        for (Core core : cores) {
            // check min deadline
            double min_deadline = Long.MAX_VALUE;
            double max_deadline = 0;
            for (Task task : core.tasks) {
                long period_us = task.period / 1000;
                if (period_us < min_deadline) {
                    min_deadline = period_us;
                }
                if (period_us > max_deadline) {
                    max_deadline = period_us;
                }
            }

            // assign nice values
            for (Task task : core.tasks) {
                long period_us = task.period / 1000;
                double deadline_ratio = (period_us - min_deadline) / (max_deadline - min_deadline);
                int nice = (int)(2 * 39 * log2(deadline_ratio + 1)) - 20;
                nice = Math.min(nice, 19);
                task.nice = nice;
                task.weight = NiceToWeight.getWeight(task.nice);
            }
            
        }
    }

    static double log2(double x) {return Math.log(x) / Math.log(2);}


    public static void assignNiceValues(List<Core> cores, double lambda) {
        for (Core core : cores) {
            // check min deadline
            long min_deadline = 10 * 1000 * 1000; // with the initial large value(10 second)
            for (Task task : core.tasks) {
                long period_us = task.period / 1000;
                if (period_us < min_deadline) {
                    min_deadline = period_us;
                }
            }
            // assign nice values
            for (Task task : core.tasks) {
                long period_us = task.period / 1000;
                task.nice = computeNice(period_us, min_deadline, lambda);
                task.weight = NiceToWeight.getWeight(task.nice);
            }
        }
    }

    public static void assignNiceValues_baseline(List<Core> cores) {
        for (Core core : cores) {
            // assign nice values
            for (Task task : core.tasks) {
                task.nice = 0;
                task.weight = NiceToWeight.getWeight(task.nice);
            }
        }
    }

    //nice_i = \min \left(-20 +  \ceil*{\log_{1.25} \frac{D_i}{D_{\text{min}}}}, \;19 \right)
    private static int computeNice(long deadline_i, long min_deadline, double lambda){
        // double relative_weight = Math.log((double)deadline_i / min_deadline) / Math.log(1.25);
        // relative_weight =  Math.ceil(relative_weight) * lambda;

        // double relative_weight = Math.log((double)deadline_i / min_deadline) / Math.log(lambda);
        double relative_weight = Math.log((double)deadline_i / min_deadline) * lambda;
        return Math.min(-20 + (int) relative_weight, 19);
    }

    public static void setTaskRandomOffset(List<Core> cores) {
        for (Core core : cores) {
            for (Task task : core.tasks) {
                task.startTime = (long) (Math.random() * (task.period/2));
            }
        }
    }
}
