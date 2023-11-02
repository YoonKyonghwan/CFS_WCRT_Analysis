#bin/bash

# set arguments
result_dir="exp_results"
schedule_simulation_method="random"
tie_comparator="BodyWCETComparator"

./gradlew build
mv ./app/build/libs/run.jar ./run.jar

rm -rf logs/*.txt*

generated_files_save_dir="generated_taskset"
file_name="1cores_15tasks_0.8utilization_43.json"
task_info_path="${generated_files_save_dir}/${file_name}"
echo "running with ${file_name}"
java -jar run.jar -t=$task_info_path -rd=$result_dir -ssm=$schedule_simulation_method -tc=$tie_comparator

