#!/bin/bash

INPUT_FILE="task_info.json"
SCHEDULERS=("CFS" "FIFO" "RR" "RM")
RESULT_DIR="./exp_results"
APPLICATION_PATH="./application"
ENABLE_NSYS=1

# If the result directory exists, remove it
if [ -d "${RESULT_DIR}" ]; then
    sudo rm -rf "${RESULT_DIR}"
fi
mkdir "${RESULT_DIR}"

for SCHEDULER in "${SCHEDULERS[@]}"
do
    OUTPUT_FILE="${SCHEDULER}_result.json"
    case "${SCHEDULER}" in
        "CFS")
            SCHED_INDEX=0
            ;;
        "FIFO")
            SCHED_INDEX=1
            ;;
        "RR")
            SCHED_INDEX=2
            ;;
        "RM")
            SCHED_INDEX=4
            ;;
        *)
            echo "Unknown scheduler: ${SCHEDULER}"
            exit 1
            ;;
    esac

    if [ ${ENABLE_NSYS} -eq 0 ]; then
        cmd="sudo ${APPLICATION_PATH} ${SCHED_INDEX} ${INPUT_FILE} ${RESULT_DIR}/${OUTPUT_FILE}"
        echo "command : ${cmd}"
        ${cmd}
    else
        nsys_cmd="sudo nsys profile -t osrt,nvtx --force-overwrite=true --run-as=root \
        --output=${RESULT_DIR}/${SCHEDULER} ${APPLICATION_PATH} ${SCHED_INDEX} ${INPUT_FILE} ${RESULT_DIR}/${OUTPUT_FILE}"
        echo "nsys command : ${nsys_cmd}"
        ${nsys_cmd}
    fi
done
