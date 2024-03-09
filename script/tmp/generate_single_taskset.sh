#!/bin/bash

# set arguments
generated_files_save_dir="generated_taskset"
num_cores=1
num_tasks=6
utilization=0.4
num_sets=2


./gradlew build
mv ./app/build/libs/run.jar ./run.jar

java -jar run.jar -gt -ns="$num_sets" -nt="$num_tasks" -nc="$num_cores" -u="$utilization" -gd="$generated_files_save_dir"
