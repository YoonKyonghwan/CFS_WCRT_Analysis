#bin/bash

# set arguments
task_info_path="app/src/main/resources/generated_taskset/1cores_3tasks_0.5utilization_0.json"
result_dir="exp_results"

# check jar file exists
# if [ ! -f "run.jar" ]; then
#     ./gradlew build
#     mv ./app/build/libs/run.jar ./run.jar
# fi

./gradlew build
mv ./app/build/libs/run.jar ./run.jar

java -jar run.jar -t=$task_info_path -rd=$result_dir


