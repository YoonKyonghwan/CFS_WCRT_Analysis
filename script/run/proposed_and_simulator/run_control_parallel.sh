#!/bin/bash

num_tasks=(2 4 6 8 10)
utilizations=(0.2 0.4 0.6 0.8)
period_set=303000

schedule_try_count=1
test_try_count=1
nice_assign="GA" # baseline, heuristic, GA
schedule_simulation_method="random" #random, random_target
taskset_dir="./generated_taskset_${period_set}"
result_dir="./exp_results_proposed_${nice_assign}_${period_set}_${schedule_simulation_method}"

rm -rf "$result_dir"

echo "build run.jar"
./gradlew build
mv ./app/build/libs/run.jar ./run.jar

start_time=$(date +%s)
start_time=$((start_time / 60))

echo "start run_exps_parallel.sh"
if [ $nice_assign = "baseline" ]; then
    lambda=0.0
    for num_task in "${num_tasks[@]}"; do
        for utilization in "${utilizations[@]}"; do
            result_dir_lambda="${result_dir}/lambda${lambda}"
            if [ $num_task -eq 2 ] || [ $num_task -eq 4 ] || [ $num_task -eq 6 ]; then
                set_start_index=(0)            
            else
                set_start_index=(0 50)
            fi
            num_test_set=$((100 / ${#set_start_index[@]}))
            for start_index in "${set_start_index[@]}"; do
                ./script/run/proposed_and_simulator/run_exps_parallel.sh $num_task $utilization $result_dir_lambda $taskset_dir $start_index $num_test_set $schedule_try_count $test_try_count $nice_assign $lambda $schedule_simulation_method &
            done
        done
    done
    wait
else 
    default_lambda=5.0
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
fi
wait
echo "finish run_exps_parallel.sh"

end_time=$(date +%s)
end_time=$((end_time / 60))
elapsed_time=$((end_time - start_time))
echo "Execution time: $elapsed_time minutes"