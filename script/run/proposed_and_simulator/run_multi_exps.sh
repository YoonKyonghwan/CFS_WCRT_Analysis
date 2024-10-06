#!/bin/bash

period_set=(101000 303000 505000 707000)
nice_assigns=("GA")
schedule_simulation_method="random"

echo "build run.jar"
./gradlew build
mv ./app/build/libs/run.jar ./run.jar

start_time=$(date +%s)
start_time=$((start_time / 60))

for period in "${period_set[@]}"; do
    for nice_assign in "${nice_assigns[@]}"; do
        result_dir="./exp_results_proposed_${nice_assign}_${period}_${schedule_simulation_method}"
        taskset_dir="./generated_taskset_${period}"
        ./script/run/proposed_and_simulator/run_multi_exps_sub.sh $result_dir $taskset_dir $nice_assign $schedule_simulation_method &
    done
done
wait
echo "all experiments are done"
end_time=$(date +%s)
end_time=$((end_time / 60))
elapsed_time=$((end_time - start_time))
echo "Total Execution time: $elapsed_time minutes"