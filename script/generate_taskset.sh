#!/bin/bash

# set arguments
generated_files_save_dir="app/src/main/resources/generated_taskset"
num_cores=1
num_tasks=(3 6 9 12 15)
utilizations=(0.3 0.4 0.5 0.6 0.7 0.8 0.9)
num_sets=30

./gradlew build
mv ./app/build/libs/run.jar ./run.jar

rm -rf "$generated_files_save_dir"

# Loop through the combinations of num_tasks and utilization
for num_task in "${num_tasks[@]}"; do
    for utilization in "${utilizations[@]}"; do
        java -jar run.jar -gt -ns="$num_sets" -nt="$num_task" -nc="$num_cores" -u="$utilization" -gd="$generated_files_save_dir"
    done
done
