import json
import os
import argparse

num_cores = [1]
num_tasks = [2, 4, 6, 8, 10]
utilizations = [0.4, 0.6, 0.8]


def genSummary(result_dir, summary_path, num_cores, num_tasks, utilizations, num_tasksets):
    header = "numCores,numTasks,utilization,tasksetIndex,realLinux_schedulability"
    with open(summary_path, "a") as f:
        f.write(header + "\n")

    for num_core in num_cores:
        for num_task in num_tasks:
            for utilization in utilizations:
                for i in range(num_tasksets):
                    result_file = result_dir + "/details/" + str(num_core) + "cores/" + str(num_task) + "tasks/" + str(utilization) + "utilization/result_" + str(i) + ".json"
                    with open(result_file, 'r') as file:
                        tasks_result = json.load(file)
                    assert len(tasks_result) == num_task
                    
                    realLinux_schedulability = True
                    for task_result in tasks_result:
                        if task_result["wcrt_ns"] > task_result["deadline_ns"]:
                            realLinux_schedulability = False
                            break
                    with open(summary_path, "a") as f:
                        f.write(str(num_core) + "," + str(num_task) + "," + str(utilization) + "," + str(i) + "," + str(realLinux_schedulability) + "\n")
    return


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--result_dir", type=str, default="./real_linux_application/exp_results_CFS", help="Directory of experiment results")
    parser.add_argument("--num_tasksets", type=int, default=50, help="Number of tasksets")
    args = parser.parse_args()
    
    # generate summary of experiment results for Table3
    result_dir = args.result_dir
    summary_path = result_dir + "/summary.csv"
    num_tasksets = args.num_tasksets

    print("Generating summary of experiment results")
    # if summary file exists, remove it
    if os.path.exists(summary_path):
        os.remove(summary_path)
    genSummary(result_dir, summary_path, num_cores, num_tasks, utilizations, num_tasksets)