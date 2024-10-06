#!/bin/bash

result_dir=$1
taskset_dir=$2
nice_assign=$3
schedule_simulation_method=$4
num_tasks=(2 4 6 8 10)
utilizations=(0.2 0.4 0.6 0.8)
schedule_try_count=1
test_try_count=1

rm -rf "$result_dir"

start_time=$(date +%s)
start_time=$((start_time / 60))

default_lambda=20.0
for num_task in "${num_tasks[@]}"; do
    for utilization in "${utilizations[@]}"; do
        if [ $num_task -eq 2 ] || [ $num_task -eq 4 ] || [ $num_task -eq 6 ]; then
            set_start_index=(0)            
        else
            set_start_index=(0 50)
        fi
        num_test_set=$((100 / ${#set_start_index[@]}))
        for start_index in "${set_start_index[@]}"; do
            ./script/run/proposed_and_simulator/run_exps_parallel.sh $num_task $utilization $result_dir $taskset_dir $start_index $num_test_set $schedule_try_count $test_try_count $nice_assign $default_lambda $schedule_simulation_method &
        done
    done
done
wait

end_time=$(date +%s)
end_time=$((end_time / 60))
elapsed_time=$((end_time - start_time))
echo "Execution time: $elapsed_time minutes"