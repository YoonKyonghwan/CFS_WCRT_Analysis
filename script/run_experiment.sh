#bin/bash

# set arguments
result_dir="exp_results"

generated_files_save_dir="app/src/main/resources/generated_taskset"
num_cores=1
num_tasks=(3 6 9 12 15)
utilizations=(0.3 0.4 0.5 0.6 0.7 0.8 0.9)
num_sets=30


./gradlew build
mv ./app/build/libs/run.jar ./run.jar

## for test
# task_info_path="tasks.json"
# java -jar run.jar -t=$task_info_path -rd=$result_dir

for num_task in "${num_tasks[@]}"; do
    for utilization in "${utilizations[@]}"; do
        for ((i=0; i<num_sets; i++)); do
            file_name="${num_cores}cores_${num_task}tasks_${utilization}utilization_${i}.json"
            task_info_path="${generated_files_save_dir}/${file_name}"
            echo "running with ${file_name}"
            java -jar run.jar -t=$task_info_path -rd=$result_dir
        done
    done
done



# need to check
# 1cores_6tasks_0.4utilization_12.json //
# 1cores_6tasks_0.8utilization_28.json
# 1cores_6tasks_0.9utilization_18.json
# 1cores_9tasks_0.8utilization_15.json
# 1cores_12tasks_0.8utilization_6.json
# 1cores_15tasks_0.6utilization_14.json
# 1cores_15tasks_0.6utilization_16.json //
# 1cores_15tasks_0.7utilization_12.json