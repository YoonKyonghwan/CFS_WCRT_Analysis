#!/bin/bash

INPUT_FILE="../../dataset/FMTV_application/generated_FMTV_json/tasks_info.json"
SIM_PERIOD_SEC=60
SCHEDULERS=("CFS" "FIFO" "RR" "RM")
RESULT_DIR="./exp_results"
APPLICATION_PATH="./application"
ENABLE_NSYS=1

# If the result directory exists, remove it
if [ -d "${RESULT_DIR}" ]; then
    sudo rm -rf "${RESULT_DIR}"
fi
mkdir "${RESULT_DIR}"


for PHASED_FLAG in ("" "-phased"); do
    for SCHEDULER in "${SCHEDULERS[@]}"; do
        OUTPUT_FILE="${SCHEDULER}${PHASED_FLAG}_result.json"
        case "${SCHEDULER}" in
            "CFS") SCHED_INDEX=0 ;;
            "FIFO") SCHED_INDEX=1 ;;
            "RR") SCHED_INDEX=2 ;;
            "RM") SCHED_INDEX=4 ;;
            *)
                echo "Unknown scheduler: ${SCHEDULER}"
                exit 1
                ;;
        esac

        cmd="${APPLICATION_PATH} ${SCHED_INDEX} ${SIM_PERIOD_SEC} ${INPUT_FILE} ${RESULT_DIR}/${OUTPUT_FILE} ${PHASED_FLAG}"
        if [ ${ENABLE_NSYS} -eq 0 ]; then
            echo "command : sudo ${cmd}"
            sudo ${cmd}
        else
            nsys_cmd="nsys profile -t osrt,nvtx --force-overwrite=true --run-as=root --output=${RESULT_DIR}/${SCHEDULER} ${cmd}"
            echo "command : ${nsys_cmd}"
            sudo ${nsys_cmd}
        fi
    done
done

