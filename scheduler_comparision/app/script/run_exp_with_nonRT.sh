#!/bin/bash

INPUT_FILE="1cores_6tasks_0.6utilization_0.json"
DATA_TYPE_INDEX=1 # 0: fmtv, 1: uunifast
SIM_PERIOD_SEC=3
SCHEDULERS=("CFS" "FIFO" "RR")
# SCHEDULERS=("CFS")
RESULT_DIR="./exp_results_non_RT"
APPLICATION_PATH="./application"
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
    echo ""
    cmd="${APPLICATION_PATH} ${SCHED_INDEX} ${SIM_PERIOD_SEC} ${INPUT_FILE} ${DATA_TYPE_INDEX} ${RESULT_DIR}/${OUTPUT_FILE} -non_RT"
    if [ ${ENABLE_NSYS} -eq 0 ]; then
        echo "command : sudo ${cmd}"
        ${cmd}
    else
        nsys_cmd="nsys profile -t osrt,nvtx --force-overwrite=true --run-as=root --output=${RESULT_DIR}/${SCHEDULER} ${cmd}"
        echo "command : ${nsys_cmd}"
        ${nsys_cmd}
    fi
done

