package org.cap.utility;

import org.cap.model.Core;
import org.cap.model.NiceToWeight;
import org.cap.model.Task;
import org.cap.model.TestConfiguration;

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

    public static void convertPeriod_us_ns(TestConfiguration testConf) {
        for (Core core: testConf.mappingInfo) {
            for (Task task: core.tasks) {
                task.period = task.period * 1000; // us -> ns
            }
        }
    }


    public static void convertPeriod_ns_us(TestConfiguration testConf) {
        for (Core core: testConf.mappingInfo) {
            for (Task task: core.tasks) {
                task.period = task.period/1000; // ns -> us
            }
        }
    }


    public static long getMaximumPeriod(List<Core> cores) {
        long maxPeriod = -1;
        for (Core core : cores) {
            for (Task task : core.tasks) {
                if (task.period > maxPeriod)
                    maxPeriod = task.period;
            }
        }
        return maxPeriod;
    }


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
            long max_deadline = 0; // with the initial large value(10 second)
            for (Task task : core.tasks) {
                long period_us = task.period / 1000;
                if (period_us > max_deadline) {
                    max_deadline = period_us;
                }
            }
            // assign nice values
            for (Task task : core.tasks) {
                long period_us = task.period / 1000;
                // int nice = computeNice(period_us, min_deadline, lambda);
                int nice = computeNiceWithMaxD(period_us, max_deadline, lambda);
                int weight = NiceToWeight.getWeight(nice);
                task.nice = nice;
                task.weight = weight;
            }

            // int max_nice = core.tasks.stream().mapToInt(task -> task.nice).max().getAsInt();
            // int shift_nice = 19 - max_nice;
            // for (Task task : core.tasks) {
            //     task.nice += shift_nice; 
            //     task.weight = NiceToWeight.getWeight(task.nice);
            // }
        }
    }

    public static void assignNiceValues_GA(List<Core> cores, List<Integer> niceValues) {
        for (Core core : cores) {
            for(Task task : core.tasks){
                task.nice = niceValues.get(task.index);
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

    private static int computeNiceWithMinD(long deadline_i, long min_deadline, double lambda){
        double relative_weight = Math.log((double)deadline_i / min_deadline) * lambda;
        int nice = (int) (-20 + relative_weight);
        assert relative_weight >= 0;
        if (relative_weight == 0.0) {
            return -20;
        }else if(relative_weight > 39){
            return 19;
        }else{
            return nice;
        }
    }


    private static int computeNiceWithMaxD(long deadline_i, long max_deadline, double lambda){
        double relative_weight = Math.log((double)deadline_i / max_deadline)* lambda;
        int nice = (int) (19 + relative_weight);
        assert relative_weight <= 0;
        if (relative_weight == 0.0) {
            return 19;
        }else if(relative_weight < -39){
            return -20;
        }else{
            return nice;
        }
    }

    public static void setTaskRandomOffset(List<Core> cores) {
        for (Core core : cores) {
            for (Task task : core.tasks) {
                long startTime = (long) (Math.random() * (task.period/2));
                // round to the nearest multiple of ms
                task.startTime = (startTime / 1000000) * 1000000;
            }
        }
    }
}
