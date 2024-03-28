#!/bin/bash

# INPUT_FILE="../../dataset/FMTV_application/generated_FMTV_json/tasks_info.json"
# INPUT_FILE="tasks_info_long.json"
# DATA_TYPE="fmtv" # "uunifest" or "fmtv"
INPUT_FILE="1cores_6tasks_0.6utilization_89.json"
DATA_TYPE="uunifest" # "uunifest" or "fmtv"
SIM_PERIOD_SEC=10
# SCHEDULERS=("CFS" "FIFO" "RR" "RM")
SCHEDULERS=("CFS")
RESULT_DIR="./exp_results"
APPLICATION_PATH="./application"
ENABLE_NSYS=1

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

# for PHASED_FLAG in "" "-phased"; do
# for PHASED_FLAG in ""; do
for PHASED_FLAG in "-phased"; do
    for SCHEDULER in "${SCHEDULERS[@]}"; do
        OUTPUT_FILE="${SCHEDULER}${PHASED_FLAG}_result.json"
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
        echo ""
        cmd="${APPLICATION_PATH} ${SCHED_INDEX} ${SIM_PERIOD_SEC} ${INPUT_FILE} ${DATA_TYPE_INDEX} ${RESULT_DIR}/${OUTPUT_FILE} ${PHASED_FLAG}"
        if [ ${ENABLE_NSYS} -eq 0 ]; then
            echo "command : sudo ${cmd}"
            ${cmd}
        else
            nsys_cmd="nsys profile -t osrt,nvtx --force-overwrite=true --run-as=root --output=${RESULT_DIR}/${SCHEDULER}${PHASED_FLAG} ${cmd}"
            echo "command : ${nsys_cmd}"
            ${nsys_cmd}
        fi
    done
done

