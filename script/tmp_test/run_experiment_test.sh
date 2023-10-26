#bin/bash

# set arguments
result_dir="exp_results"

./gradlew build
mv ./app/build/libs/run.jar ./run.jar


generated_files_save_dir="app/src/main/resources/generated_taskset"
file_name="1cores_6tasks_0.8utilization_28.json"
task_info_path="${generated_files_save_dir}/${file_name}"
echo "running with ${file_name}"
java -jar run.jar -t=$task_info_path -rd=$result_dir

# java -jar run.jar -t=app/src/main/resources/generated_taskset/1cores_6tasks_0.8utilization_28.json -rd=exp_results



# need to check
# 1cores_6tasks_0.4utilization_12.json //
# 1cores_6tasks_0.8utilization_28.json
# 1cores_6tasks_0.9utilization_18.json
# 1cores_9tasks_0.8utilization_15.json
# 1cores_12tasks_0.8utilization_6.json
# 1cores_15tasks_0.6utilization_14.json
# 1cores_15tasks_0.6utilization_16.json //
# 1cores_15tasks_0.7utilization_12.json