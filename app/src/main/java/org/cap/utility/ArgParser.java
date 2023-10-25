package org.cap.utility;

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
                .help("task info file path");
        parser.addArgument("--resultDir", "-rd")
                .dest("result_dir")
                .type(String.class)
                .help("directory to store result files");
        Namespace params = parser.parseArgsOrFail(args);
        return params;
    }
}
