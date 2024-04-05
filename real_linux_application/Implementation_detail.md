# WCRT analysis for CFS in Linux systems

## Overview

Given that the proposed method provides conservative estimates of the WCRTs, it is necessary to examine the degree of over-estimation. To evaluate the accuracy of the proposed WCRT analysis method, its schedulability estimations are compared with the actual schedulability observed from executing tasks on a real Linux system. For a direct comparison with real Linux systems, we first measure the actual WCRTs by running tasks in a real Linux environment. For each task set, we run all tasks for 90 seconds at the same time, measuring the response time of all jobs. We then identify the longest response time for each task, which serves as the actual WCRT. Subsequently, we assess the schedulability of the task sets and compare these results with the schedulability analysis by our proposed method. However, measuring the WCRT by actually running tasks in real Linux systems is a time-intensive endeavor. As a result, for each configuration, we decide to randomly select 30 task sets from the task set described in Section V-A1, and we conduct our schedulability analysis for these chosen sets.

## Implementation Detail
The application comprises three C code files, each serving a specific role in the system simulation.

### [main.c](./real_linux_application/app/src/main.c)
This is the entry point of the application and handles the following key tasks:

1. **Input Information**
    - Receives scheduling policy, simulation duration, system information, and the path for saving the result file.
2. **Extracting Task Information**
    - Extracts key details from the system information, such as task cycles and execution times.
3. **Task Creation and Execution**
    - Uses the pthread library for creating and executing tasks.
4. **Processor Affinity**
    - Ensures tasks do not migrate between cores during execution by setting processor affinity with pthread_attr_t.
5. **Simulation Termination and Result Storage**
    - Terminates tasks after the set simulation period and saves the WCRT analysis results.

### [task.c](./real_linux_application/app/src/task.c)
This file implements task_function, crucial for real-time system simulation:

1. **Task Initialization**
    - Initializes necessary variables
    - sets scheduling policy, priority, and nice value for each task.
2. **Initial Synchronization**
    - Synchronizes thread start times with global_start_time using clock_nanosleep.
    - Initially, pthread_barrier was used for initial synchronization. However, experiments revealed that this method based on signals doesn't synchronize threads effectively when the number of tasks increases. As a result, the implementation was shifted to using clock_nanosleep for more reliable synchronization.
3. **Iterative Job Execution**
    - Each loop iteration corresponds to a single task job.
    - Performs workloads using busyWait.
    - Calculates response times.
4. * **Scheduling Next Job**
    - If a job completes before its period, the next job's start is scheduled after the period.
    - Otherwise, it starts the next job immediately.

### [util.c](./real_linux_application/app/src/util.c)
Contains utility functions used across main.c and task.c, providing essential functionality to both files.