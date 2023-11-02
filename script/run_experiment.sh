#bin/bash

# set arguments
result_dir="exp_results"

generated_files_save_dir="generated_taskset"
num_cores=(1)
num_tasks=(3 6 9 12 15)
utilizations=(0.2 0.4 0.6 0.8)
num_sets=50

schedule_simulation_method="priority-queue"
tie_comparator="PeriodComparator"


./gradlew build
mv ./app/build/libs/run.jar ./run.jar

rm -rf "$result_dir"

# check total execution time
start_time="$(date -u +%s)"

for num_core in "${num_cores[@]}"; do
    for num_task in "${num_tasks[@]}"; do
        for utilization in "${utilizations[@]}"; do
            for ((i=0; i<num_sets; i++)); do
                file_name="${num_core}cores_${num_task}tasks_${utilization}utilization_${i}.json"
                task_info_path="${generated_files_save_dir}/${num_core}cores/${num_task}tasks/${utilization}utilization/${file_name}"
                echo "running with ${file_name}"
                java -jar run.jar -t=$task_info_path -rd=$result_dir -ssm=$schedule_simulation_method -tc=$tie_comparator
            done
        done
    done
done

end_time="$(date -u +%s)"
elapsed="$(($end_time-$start_time))"
echo "Total of $elapsed seconds elapsed for the experiment"
