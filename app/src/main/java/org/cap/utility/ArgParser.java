package org.cap.utility;

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
        parser.addArgument("--genTasks", "-gt")
                .dest("gen_tasks")
                .action(Arguments.storeTrue())
                .help("generate tasks and exit");
        parser.addArgument("--num_sets", "-ns")
                .dest("num_sets")
                .type(Integer.class)
                .help("number of taskset to generate");
        parser.addArgument("--num_tasks", "-nt")
                .dest("num_tasks")
                .type(Integer.class)
                .help("number of tasks in a taskset");
        parser.addArgument("--num_cores", "-nc")
                .dest("num_cores")
                .type(Integer.class)
                .help("number of cores in a system");
        parser.addArgument("--utilization", "-u")
                .dest("utilization")
                .type(Double.class)
                .help("cpu utilization of tasks");
        parser.addArgument("--generated_files_save_dir", "-gd")
                .dest("generated_files_save_dir")
                .type(String.class)
                .help("directory to store generated files");

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
                .help("search method (either brute-force, random, or priority-queue) ");
        parser.addArgument("--simulation_time", "-st")
                .dest("simulation_time")
                .type(Long.class)
                .setDefault(0L)
                .nargs("?")
                .help("simulation time (0 for hyper period, -1 for max period, other positive integer values are used as an exact value) ");
        parser.addArgument("--tie_comparator", "-tc")
                .dest("tie_comparator")
                .type(Arguments.enumStringType(ComparatorCase.class))
                .setDefault(ComparatorCase.FIFO.getClassName())
                .nargs("?")
                .help("tie comparator when the virtual runtime is same (BodyWCETComparator, PeriodComparator, WeightComparator, FIFOComparator, ReleaseTimeComparator or UnorderedComparator). If schedule simulation method is brute-force, tie comparator is fixed to UnorderedComparator.");
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
        parser.addArgument("--wakeup_granularity", "-wg")
                .dest("wakeup_granularity")
                .type(Integer.class)
                .setDefault(0)
                .nargs("?")
                .help("The wakeup granularity");
        Namespace params = parser.parseArgsOrFail(args);
        return params;
    }
}
