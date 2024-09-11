#!/bin/bash

num_tasks=(2 4 6 8 10)
utilizations=(0.2 0.4 0.6 0.8)
# result_dir="./exp_results_proposed_new_nice_assignment"
# result_dir="./exp_results_proposed_new_overestimation"
result_dir="./exp_results_proposed_new_fix_lambda"
# taskset_dir="./generated_taskset_even"
taskset_dir="./generated_taskset_10010000"
schedule_try_count=1
test_try_count=1
fix_lambda="true" # false: heuristic, true: fix_lambda

rm -rf "$result_dir"
echo "remove $result_dir"

echo "build run.jar"
./gradlew build
mv ./app/build/libs/run.jar ./run.jar

echo "start run_exps_parallel.sh"
if [ $fix_lambda = "true" ]; then
    fixed_lambdas=(0.0 1.0 2.0 3.0 4.0 5.0 6.0 7.0 8.0 9.0 10.0 11.0 12.0 13.0 14.0 15.0 16.0 17.0 18.0 19.0 20.0 21.0 22.0 23.0 24.0 25.0 26.0 27.0 28.0 29.0 30.0 31.0 32.0 33.0 34.0 35.0 36.0 37.0 38.0 39.0 40.0)
    for lambda in "${fixed_lambdas[@]}"; do
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
                    ./script/run/additional/run_exps_parallel.sh $num_task $utilization $result_dir_lambda $taskset_dir $start_index $num_test_set $schedule_try_count $test_try_count $fix_lambda $lambda &
                done
            done
        done
        wait
    done
else # heuristic algorithm to assign nice_value
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
                ./script/run/additional/run_exps_parallel.sh $num_task $utilization $result_dir $taskset_dir $start_index $num_test_set $schedule_try_count $test_try_count $fix_lambda $default_lambda &
            done
        done
    done
fi
wait
echo "finish run_exps_parallel.sh"