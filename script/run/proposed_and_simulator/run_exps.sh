#!/bin/bash

# set arguments
num_cores=(1)
num_tasks=(2 4 6 8 10)
utilizations=(0.2 0.4 0.6 0.8)
target_latency=18000
min_gran=2250
jiffy=1000
nice_assign="GA" # baseline, heuristic, GA

num_sets=100
period_set=303000
taskset_dir="./generated_taskset_${period_set}"
result_dir="./exp_results_proposed_time_consumption_${nice_assign}_${period_set}"
schedule_simulation_method="random" # random, random_target_task
schedule_try_count=1
test_try_count=1000
lambda=5.0

./gradlew build
mv ./app/build/libs/run.jar ./run.jar

rm -rf "$result_dir"

# check total execution time
start_time=$(date +%s)
start_time=$((start_time / 60))

for num_core in "${num_cores[@]}"; do
    for num_task in "${num_tasks[@]}"; do
        for utilization in "${utilizations[@]}"; do
            for ((i=0; i<num_sets; i++)); do
                file_name="${num_core}cores_${num_task}tasks_${utilization}utilization_${i}.json"
                task_info_path="${taskset_dir}/${num_core}cores/${num_task}tasks/${utilization}utilization/${file_name}"
                cmd="java -jar run.jar -t=$task_info_path -rd=$result_dir -ssm=$schedule_simulation_method -stc=$schedule_try_count -ttc=$test_try_count -tl=$target_latency -mg=$min_gran -jf=$jiffy -nat=$nice_assign -nl=$lambda -lo=off"
                echo $cmd
                ${cmd}
            done
        done
    done
done

end_time=$(date +%s)
end_time=$((end_time / 60))
elapsed_time=$((end_time - start_time))
echo "Execution time: $elapsed_time minutes"

rm -rf logs/*.txt*
