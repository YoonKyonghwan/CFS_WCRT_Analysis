#!/bin/bash

# set arguments
num_core=1
num_task=$1
utilization=$2
result_dir=$3
taskset_dir=$4
start_index=$5
num_test_set=$6
schedule_try_count=$7
test_try_count=$8
fix_lambda=$9
lambda=${10}

schedule_simulation_method="random" 
target_latency=18000
min_gran=2250
jiffy_us=1000

echo "Start // num_task: $num_task, utilization: $utilization, start_index: $start_index, num_test_set: $num_test_set"
for ((i=start_index; i<start_index+num_test_set; i++)); do
    file_name="${num_core}cores_${num_task}tasks_${utilization}utilization_${i}.json"
    task_info_path="${taskset_dir}/${num_core}cores/${num_task}tasks/${utilization}utilization/${file_name}"
    cmd="java -jar run.jar -t=$task_info_path -rd=$result_dir -ssm=$schedule_simulation_method -stc=$schedule_try_count -ttc=$test_try_count -tl=$target_latency -mg=$min_gran -jf=$jiffy_us -fl=$fix_lambda -nl=$lambda -lo=off"
    # echo $cmd
    ${cmd}
done
echo "Finish // num_task: $num_task, utilization: $utilization"

rm -rf logs/*.txt*
