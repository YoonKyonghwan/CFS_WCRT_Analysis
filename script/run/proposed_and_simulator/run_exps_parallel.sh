#!/bin/bash

num_tasks=(4 8 12 16 20)
utilizations=(0.2 0.4 0.6 0.8)
nice_assign="GA" # baseline, heuristic, GA

schedule_try_count=1
test_try_count=1000
period_set=303000
taskset_dir="./generated_taskset_${period_set}"
schedule_simulation_method="random" #random, random_target
result_dir="./exp_results_proposed_${nice_assign}_${period_set}_${schedule_simulation_method}_new"

rm -rf "$result_dir"

echo "build run.jar"
./gradlew build
mv ./app/build/libs/run.jar ./run.jar

start_time=$(date +%s)
start_time=$((start_time / 60))

echo "start experiments in parallel"
if [ $nice_assign = "baseline" ]; then
    default_lambda=0.0
else 
    default_lambda=5.0
fi

for num_task in "${num_tasks[@]}"; do
    for utilization in "${utilizations[@]}"; do
        if [ $num_task -eq 2 ] || [ $num_task -eq 4 ] || [ $num_task -eq 6 ]; then
            set_start_index=(0)
        else
            set_start_index=(0 50)
        fi
        num_test_set=$((100 / ${#set_start_index[@]}))
        for start_index in "${set_start_index[@]}"; do
            ./script/run/proposed_and_simulator/run_exps_sub_parallel.sh $num_task $utilization $result_dir $taskset_dir $start_index $num_test_set $schedule_try_count $test_try_count $nice_assign $default_lambda $schedule_simulation_method &
        done
    done
done
wait
echo "finish all experiments"

end_time=$(date +%s)
end_time=$((end_time / 60))
elapsed_time=$((end_time - start_time))
echo "Execution time: $elapsed_time minutes"
