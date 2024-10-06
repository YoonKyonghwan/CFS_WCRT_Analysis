#!/bin/bash

INPUT_DIR="./generated_taskset_303000_updated"
RESULT_DIR="./real_linux_application/exp_results_CFS"

NUM_REPEAT=1
# SCHEDULER="CFS"
SCHED_INDEX=0
APPLICATION_PATH="./real_linux_application/app/application"

num_cores=(1)
num_tasks=(2 4 6 8 10)
utilizations=(0.2 0.4 0.6 0.8)
num_sets=100

bash script/run/real_linux/build_application.sh

if [ ! -d ${RESULT_DIR} ]; then
    mkdir ${RESULT_DIR}
fi
rm -rf ${RESULT_DIR}/*

start_time=$(date +%s)
start_time=$((start_time / 60))

for num_core in "${num_cores[@]}"; do
    for num_task in "${num_tasks[@]}"; do
        for utilization in "${utilizations[@]}"; do
            OUTPUT_DIR="${RESULT_DIR}/details/${num_core}cores/${num_task}tasks/${utilization}utilization"
            if [ ! -d ${OUTPUT_DIR} ]; then
                mkdir -p ${OUTPUT_DIR}
            fi

            for ((i=0; i<num_sets; i++)); do
                INPUT_FILE_NAME="${num_core}cores_${num_task}tasks_${utilization}utilization_${i}.json"
                INPUT_PATH="${INPUT_DIR}/${num_core}cores/${num_task}tasks/${utilization}utilization/${INPUT_FILE_NAME}"
                echo "running with ${INPUT_FILE_NAME}"
                OUTPUT_FILE="result_${i}.json"
                cmd="${APPLICATION_PATH} ${SCHED_INDEX} ${NUM_REPEAT} ${INPUT_PATH} ${OUTPUT_DIR}/${OUTPUT_FILE}"
                echo ""
                echo "command : ${cmd}"
                ${cmd}
            done
        done
    done
done

end_time=$(date +%s)
end_time=$((end_time / 60))
elapsed_time=$((end_time - start_time))
echo "Execution time: $elapsed_time minutes"

python3 ./script/run/real_linux/gen_exp_summary.py --result_dir=${RESULT_DIR} --num_tasksets=${num_sets} 