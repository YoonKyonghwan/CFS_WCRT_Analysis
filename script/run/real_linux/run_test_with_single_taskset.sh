#!/bin/bash

INPUT_FILE="real_linux_application/app/sample_taskset.json"
SIM_PERIOD_SEC=10
# SCHEDULERS=("CFS" "FIFO" "RR")
SCHEDULERS=("CFS")
RESULT_DIR="./real_linux_application/exp_results_single"
APPLICATION_PATH="./real_linux_application/app/application"
ENABLE_NSYS=0

# If the result is not exist, create the directory
if [ ! -d ${RESULT_DIR} ]; then
    mkdir ${RESULT_DIR}
fi
rm -f ${RESULT_DIR}/*

for SCHEDULER in "${SCHEDULERS[@]}"; do
    OUTPUT_FILE="${SCHEDULER}_result.json"
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
    
    echo ""
    cmd="${APPLICATION_PATH} ${SCHED_INDEX} ${SIM_PERIOD_SEC} ${INPUT_FILE} ${RESULT_DIR}/${OUTPUT_FILE}"
    if [ ${ENABLE_NSYS} -eq 0 ]; then
        echo "command : sudo ${cmd}"
        ${cmd}
    else
        nsys_cmd="nsys profile -t osrt,nvtx --force-overwrite=true --run-as=root --output=${RESULT_DIR}/${SCHEDULER} ${cmd}"
        echo "command : ${nsys_cmd}"
        ${nsys_cmd}
    fi
done

