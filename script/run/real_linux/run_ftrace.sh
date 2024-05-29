#!/bin/bash

echo 0 > /sys/kernel/debug/tracing/tracing_on
sleep 1
echo "tracing_off"

echo 0 > /sys/kernel/debug/tracing/events/enable
sleep 1
echo "events disabled"

echo pick_eevdf > /sys/kernel/debug/tracing/set_ftrace_filter #dummy
sleep 1
echo "set_ftrace_filter_init"

echo function > /sys/kernel/debug/tracing/current_tracer
sleep 1
echo "current_tracing is set to function_graph"

echo 1 > /sys/kernel/debug/tracing/events/sched/sched_wakeup/enable
echo 1 > /sys/kernel/debug/tracing/events/sched/sched_switch/enable
#echo 1 > /sys/kernel/debug/tracing/events/sched/sched_stat_runtime/enable

#echo 1 > /sys/kernel/debug/tracing/events/irq/irq_handler_entry/enable
#echo 1 > /sys/kernel/debug/tracing/events/irq/irq_handler_exit/enable

#echo 1 > /sys/kernel/debug/tracing/events/raw_syscalls/enable
sleep 1
echo "event enabled"

echo resched_curr preempt_schedule_irq > /sys/kernel/debug/tracing/set_ftrace_filter
sleep 1
echo "set_ftrace_filter"

echo 1 > /sys/kernel/debug/tracing/options/func_stack_trace
sleep 1
echo "function stack trace enabled"

echo 1 > /sys/kernel/debug/tracing/tracing_on
echo "tracing on"




echo "run program"

WORK_DIR=/home/ykw6644/workspace/cfs-wcrt-simulator
cd ${WORK_DIR}
# command
sudo ./real_linux_application/app/application 0 1 real_linux_application/app/sample_taskset.json ./real_linux_application/exp_results_single/EEVDF_result.json
#sudo nsys profile -t osrt,nvtx --force-overwrite=true --run-as=root --output=./real_linux_application/exp_results_single/EEVDF ./real_linux_application/app/application 0 1 real_linux_application/app/sample_taskset.json ./real_linux_application/exp_results_single/EEVDF_result.json


echo 0 > /sys/kernel/debug/tracing/tracing_on
echo "tracing_off"
sleep 1

echo 0 > /sys/kernel/debug/tracing/events/sched/sched_wakeup/enable
echo 0 > /sys/kernel/debug/tracing/events/sched/sched_switch/enable
echo 0 > /sys/kernel/debug/tracing/events/sched/sched_stat_runtime/enable

echo 0 > /sys/kernel/debug/tracing/events/irq/irq_handler_entry/enable
echo 0 > /sys/kernel/debug/tracing/events/irq/irq_handler_exit/enable

echo 0 > /sys/kernel/debug/tracing/events/raw_syscalls/enable
sleep 1
echo "event disabled"

echo 0 > /sys/kernel/debug/tracing/options/func_stack_trace
sleep 1
echo "function stack trace disabled"

RESULT_DIR=/home/ykw6644/workspace/cfs-wcrt-simulator/real_linux_application/exp_results_single/
cp /sys/kernel/debug/tracing/per_cpu/cpu3/trace ${RESULT_DIR}
mv ${RESULT_DIR}/trace ${RESULT_DIR}/ftrace.log
echo "save file to ${RESULT_DIR}"
sudo chmod +rw ${RESULT_DIR}/ftrace.log

