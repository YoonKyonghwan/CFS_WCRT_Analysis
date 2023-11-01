#bin/bash

# set arguments
result_dir="exp_results"
schedule_simulation_method="priority-queue"
tie_comparator="PeriodComparator"

./gradlew build
mv ./app/build/libs/run.jar ./run.jar


generated_files_save_dir="generated_taskset"
file_name="1cores_3tasks_0.6utilization_30.json"
task_info_path="${generated_files_save_dir}/${file_name}"
echo "running with ${file_name}"
java -jar run.jar -t=$task_info_path -rd=$result_dir -ssm=$schedule_simulation_method -tc=$tie_comparator

