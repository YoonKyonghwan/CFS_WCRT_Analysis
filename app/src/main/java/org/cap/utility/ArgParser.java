package org.cap.utility;

import org.cap.model.NiceAssignMethod;
import org.cap.model.ScheduleSimulationMethod;
import org.cap.simulation.comparator.ComparatorCase;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;

public class ArgParser {
    public Namespace parseArgs(String[] args) {
        ArgumentParser parser = ArgumentParsers.newFor("Main").build()
                .defaultHelp(true)
                .description("Simulate CFS and compute the proposed schedulability test");
        parser.addArgument("--task_info_path", "-t")
                .dest("task_info_path")
                .type(String.class)
                .nargs("?")
                .help("task info file path");
        parser.addArgument("--resultDir", "-rd")
                .dest("result_dir")
                .type(String.class)
                .setDefault(".")
                .help("directory to store result files");
        parser.addArgument("--schedule_simulation_method", "-ssm")
                .dest("schedule_simulation_method")
                .type(Arguments.enumStringType(ScheduleSimulationMethod.class))
                .setDefault(ScheduleSimulationMethod.BRUTE_FORCE.toString())
                .nargs("?")
                .help("search method (either brute-force, random, random_target_task, or priority-queue) ");
        parser.addArgument("--simulation_time", "-st")
                .dest("simulation_time")
                .type(Long.class)
                .setDefault(0L)
                .nargs("?")
                .help("simulation time (0 for hyper period * 2, -1 for max period, other positive integer values are used as an exact value) ");
        parser.addArgument("--tie_comparator", "-tc")
                .dest("tie_comparator")
                .type(Arguments.enumStringType(ComparatorCase.class))
                .setDefault(ComparatorCase.FIFO.getClassName())
                .nargs("?")
                .help("tie comparator when the virtual runtime is same (BodyWCETComparator, PeriodComparator, WeightComparator, TargetTaskBasedComparator, InitialOrderBasedComparator, FIFOComparator, ReleaseTimeComparator or UnorderedComparator). If schedule simulation method is brute-force, tie comparator is fixed to UnorderedComparator.");
        parser.addArgument("--additional_comparator", "-ac")
                .dest("additional_comparator")
                .type(Arguments.enumStringType(ComparatorCase.class))
                .setDefault(new String [] {"FIFOComparator", "InitialOrderComparator"})
                .nargs("+")
                .help("additional comparators when the primaray comparator is same (BodyWCETComparator, PeriodComparator, WeightComparator, TargetTaskBasedComparator, InitialOrderComparator, FIFOComparator, ReleaseTimeComparator or UnorderedComparator).");
        parser.addArgument("--schedule_try_count", "-stc")
                .dest("schedule_try_count")
                .type(Long.class)
                .setDefault(1000L)
                .nargs("?")
                .help("The number of tries to perform a schedule (this option is only valid for the random option of schedule_simulation_method) ");
        parser.addArgument("--logger_option", "-lo")
                .dest("logger_option")
                .type(String.class)
                .setDefault("off")
                .nargs("?")
                .help("logger option (off, info, fine)");
        parser.addArgument("--test_try_count", "-ttc")
                .dest("test_try_count")
                .type(Integer.class)
                .setDefault(1)
                .nargs("?")
                .help("The number of tries to perform a test");
        parser.addArgument("--target_latency", "-tl")
                .dest("target_latency")
                .type(Integer.class)
                .setDefault(20000)
                .nargs("?")
                .help("The target latency");
        parser.addArgument("--minimum_granularity", "-mg")
                .dest("minimum_granularity")
                .type(Integer.class)
                .setDefault(1)
                .nargs("?")
                .help("The minimum granularity");
        parser.addArgument("--jiffy", "-jf")
                .dest("jiffy")
                .type(Integer.class)
                .setDefault(1000)
                .nargs("?")
                .help("jiffy(us)");
        parser.addArgument("--nice_lambda", "-nl")
                .dest("nice_lambda")
                .type(Double.class)
                .setDefault(5.0)
                .nargs("?")
                .help("nice_lambda");
        parser.addArgument("--wakeup_granularity", "-wg")
                .dest("wakeup_granularity")
                .type(Integer.class)
                .setDefault(0)
                .nargs("?")
                .help("The wakeup granularity");
        parser.addArgument("--initial_order", "-io")
                .dest("initial_order")
                .type(Boolean.class)
                .setDefault(true)
                .nargs("?")
                .help("Change initial order");
        parser.addArgument("--nice_assign_method", "-nat")
                .dest("nice_assign_method")
                .type(Arguments.enumStringType(NiceAssignMethod.class))
                .setDefault(NiceAssignMethod.HEURISTIC.toString())
                .nargs("?")
                .help("nice value method (baseline, heuristic, GA) ");
        Namespace params = parser.parseArgsOrFail(args);
        return params;
    }
}
