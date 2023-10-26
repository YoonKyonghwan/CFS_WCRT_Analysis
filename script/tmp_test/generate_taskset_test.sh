#!/bin/bash

# set arguments
generated_files_save_dir="app/src/main/resources/generated_taskset"
num_cores=1
num_tasks=15
utilizations=0.5
num_sets=2

./gradlew build
mv ./app/build/libs/run.jar ./run.jar


java -jar run.jar -gt -ns="$num_sets" -nt="$num_tasks" -nc="$num_cores" -u="$utilizations" -gd="$generated_files_save_dir"
