#!/bin/bash

# set arguments
result_dir="exp_results_test_20240326_20ms"

generated_files_save_dir="generated_taskset_new_100_20240328_20ms"
num_cores=(1)
num_tasks=(3 6 9 12)
utilizations=(0.4 0.6 0.8)
num_sets=100


#schedule_simulation_method="random_target_task" 
schedule_simulation_method="random" 
schedule_try_count=1
test_try_count=10000
target_latency=18000
min_gran=2250
jiffy_us=1000

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
		        #echo "java -jar run.jar -t=$task_info_path -rd=$result_dir -ssm=$schedule_simulation_method -stc=$schedule_try_count -ttc=$test_try_count -lo=info"
                java -jar run.jar -t=$task_info_path -rd=$result_dir -ssm=$schedule_simulation_method -stc=$schedule_try_count -ttc=$test_try_count -tl=$target_latency -mg=$min_gran -jf=$jiffy_us
            done
        done
    done
done

end_time="$(date -u +%s)"
elapsed="$(($end_time-$start_time))"
echo "Total of $elapsed seconds elapsed for the experiment"

rm -rf logs/*.txt*
