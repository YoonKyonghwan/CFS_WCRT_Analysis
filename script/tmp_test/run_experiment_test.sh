#bin/bash

# set arguments
result_dir="exp_results_tmp"
schedule_simulation_method="priority-queue"
tie_comparator="PeriodComparator"

./gradlew build
mv ./app/build/libs/run.jar ./run.jar

rm -rf logs/*.txt*

num_core=1
num_task=12
utilization=0.6
dataset_index=42

generated_files_save_dir="generated_taskset/${num_core}cores/${num_task}tasks/${utilization}utilization"
file_name="${num_core}cores_${num_task}tasks_${utilization}utilization_${dataset_index}.json"
task_info_path="${generated_files_save_dir}/${file_name}"
echo "running with ${task_info_path}"
java -jar run.jar -t=$task_info_path -rd=$result_dir -ssm=$schedule_simulation_method -tc=$tie_comparator -lo=fine

