#bin/bash

# set arguments
result_dir="exp_results"
generated_files_save_dir="app/src/main/resources/generated_taskset"
num_cores=1
num_tasks=(3 5 7 9 11)
utilizations=(0.3 0.4 0.5 0.6 0.7 0.8)
num_sets=10

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




