#bin/bash

# set arguments
result_dir="exp_results"
schedule_simulation_method="priority-queue"
tie_comparator="PeriodComparator"

./gradlew build
mv ./app/build/libs/run.jar ./run.jar


# generated_files_save_dir="app/src/main/resources/generated_taskset"
# file_name="1cores_3tasks_0.9utilization_6.json"
generated_files_save_dir="."
file_name="1cores_5tasks_0.5utilization_0.json"
task_info_path="${generated_files_save_dir}/${file_name}"
echo "running with ${file_name}"
java -jar run.jar -t=$task_info_path -rd=$result_dir -ssm=$schedule_simulation_method -tc=$tie_comparator


# java -jar run.jar -t=app/src/main/resources/generated_taskset/1cores_6tasks_0.8utilization_28.json -rd=exp_results -ssm=priority-queue -tc=BodyWCETComparator
