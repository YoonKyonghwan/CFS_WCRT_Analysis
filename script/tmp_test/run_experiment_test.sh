#bin/bash

# set arguments
result_dir="exp_results"
generated_files_save_dir="generated_taskset"
schedule_simulation_method="random"   # "priority-queue"
schedule_try_count=1000

./gradlew build
mv ./app/build/libs/run.jar ./run.jar

rm -rf logs/*.txt*

num_core=1
num_task=9
utilization=0.4
dataset_index=1

generated_files_save_dir="${generated_files_save_dir}/${num_core}cores/${num_task}tasks/${utilization}utilization"
file_name="${num_core}cores_${num_task}tasks_${utilization}utilization_${dataset_index}.json"
task_info_path="${generated_files_save_dir}/${file_name}"

# check total execution time
start_time="$(date -u +%s)"

echo "running with ${task_info_path}"
java -jar run.jar -t=$task_info_path -rd=$result_dir -ssm=$schedule_simulation_method -stc=$schedule_try_count -lo=info


end_time="$(date -u +%s)"
elapsed="$(($end_time-$start_time))"
echo "Total of $elapsed seconds elapsed for the experiment"