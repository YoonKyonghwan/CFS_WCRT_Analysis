#!/bin/bash

INPUT_DIR="./generated_taskset_new_100_20240328_30ms"
RESULT_DIR="./exp_results"

DATA_TYPE="uunifest" # "uunifest" or "fmtv"
SIM_PERIOD_SEC=60
SCHEDULER="CFS"
APPLICATION_PATH="./application"
ENABLE_NSYS=0

num_cores=(1)
num_tasks=(3 6 9 12)
utilizations=(0.4 0.6 0.8)
num_sets=100

# If the result is not exist, create the directory
if [ ! -d ${RESULT_DIR} ]; then
    mkdir ${RESULT_DIR}
fi
rm -f ${RESULT_DIR}/*


# set Data Type index
case "${DATA_TYPE}" in
    "fmtv") DATA_TYPE_INDEX=0 ;;
    "uunifest") DATA_TYPE_INDEX=1 ;;
    *)
        echo "Unsupported data type: ${DATA_TYPE}"
        exit 1
        ;;
esac
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
            # create the directory
            OUTPUT_DIR="${RESULT_DIR}/details/${num_core}cores/${num_task}tasks/${utilization}utilization"
            if [ ! -d ${OUTPUT_DIR} ]; then
                mkdir -p ${OUTPUT_DIR}
            fi

            for ((i=0; i<num_sets; i++)); do
                INPUT_FILE_NAME="${num_core}cores_${num_task}tasks_${utilization}utilization_${i}.json"
                INPUT_PATH="${INPUT_DIR}/${num_core}cores/${num_task}tasks/${utilization}utilization/${INPUT_FILE_NAME}"
                echo "running with ${INPUT_FILE_NAME}"
                OUTPUT_FILE="result_${i}.json"
                cmd="${APPLICATION_PATH} ${SCHED_INDEX} ${SIM_PERIOD_SEC} ${INPUT_PATH} ${DATA_TYPE_INDEX} ${OUTPUT_DIR}/${OUTPUT_FILE}"
                echo ""
                echo ""
                echo "command : ${cmd}"
                ${cmd}
            done
        done
    done
done

