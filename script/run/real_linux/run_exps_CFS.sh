#!/bin/bash

INPUT_DIR="./generated_taskset_10010000_updated"
RESULT_DIR="./real_linux_application/exp_results_CFS"

NUM_REPEAT=1
SCHEDULER="CFS"
APPLICATION_PATH="./real_linux_application/app/application"

num_cores=(1)
num_tasks=(2 4 6 8 10)
utilizations=(0.2 0.4 0.6 0.8)
num_sets=100

# If the result is not exist, create the directory
if [ ! -d ${RESULT_DIR} ]; then
    mkdir ${RESULT_DIR}
fi
rm -rf ${RESULT_DIR}/*

case "${SCHEDULER}" in
    "CFS") SCHED_INDEX=0 ;;
    "FIFO") SCHED_INDEX=1 ;;
    "RR") SCHED_INDEX=2 ;;
    "EDF") SCHED_INDEX=3 ;;
    "RM") SCHED_INDEX=4 ;;
    *)
        echo "Unknown scheduler: ${SCHEDULER}"
        exit 1
        ;;
esac

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

python3 ./script/run/real_linux/gen_exp_summary.py --result_dir=${RESULT_DIR} --num_tasksets=${num_sets} 