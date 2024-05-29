#!/bin/bash

output_dir="output"
rm -rf "$output_dir"
mkdir -p "$output_dir"

while true; do
    timestamp=$(date +"%S")
    output_file="${output_dir}/cpu3_info_${timestamp}.txt"
    
    tail -n 300 /sys/kernel/debug/sched/debug > ${output_file}
    
    sleep 1
done

