#bin/bash

# set arguments
num_sets=5
num_tasks=3
num_cores=1
utilization=0.5
generated_files_save_dir="app/src/main/resources/generated_taskset"

./gradlew build
mv ./app/build/libs/run.jar ./run.jar

# ./gradlew run --args="-gt -ns=$num_sets -nt=$num_tasks -nc=$num_cores -u=$utilization -gd=$generated_files_save_dir"
java -jar run.jar -gt -ns=$num_sets -nt=$num_tasks -nc=$num_cores -u=$utilization -gd=$generated_files_save_dir


