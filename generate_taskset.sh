#bin/bash

# set arguments
num_sets=3
num_tasks=3
num_cores=1
utilization=0.5
generated_files_save_dir="./app/src/main/resources/generated_taskset"

./gradlew run --args="-gt -ns $num_sets -nt $num_tasks -nc $num_cores -u $utilization -gd $generated_files_save_dir"

./gradlew run --args="-gt -ns 3 -nt 3 -nc 1 -u 0.5 -gd ./app/src/main/resources/generated_taskset"

