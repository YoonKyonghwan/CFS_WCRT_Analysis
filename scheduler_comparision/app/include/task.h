#define _GNU_SOURCE
#pragma once
#include <pthread.h>
#include <time.h>
#include <unistd.h>
#include <sched.h>
#include <stdbool.h>
#include <sys/syscall.h> // for syscall(SYS_gettid)
#include <sys/resource.h> // for setpriority
#include "scheduler.h"

#define USE_NVTX

#ifdef USE_NVTX
#include <nvtx3/nvToolsExt.h>
#endif


#ifdef USE_NVTX
#define PUSH_PROFILE(name) nvtxRangePush(name);
#define POP_PROFILE() nvtxRangePop();
#else
#define PUSH_PROFILE(name)
#define POP_PROFILE()
#endif

typedef struct {
    char *name;
    char core_index;
    char sched_policy;
    char priority;
    bool isRTTask;

    bool isPeriodic;
    long long period_ns;
    int low_interarrival_time_ns;
    int upper_interarrival_time_ns;

    int phased_read_time_ns;
    int phased_write_time_ns;
    int *phased_execution_time_ns;

    int *runnables_read_time_ns;
    int **runnables_execution_time_ns;
    int *runnables_write_time_ns;

    int num_samples;
    int num_runnables;

    long long wcet_ns;
    long long *response_time_ns;
    long long wcrt_ns;
    
} Task_Info;

void *task_function(void *arg);

void runRunnable(int read_ns, int execution_ns, int write_ns);
void memoryAccess(int time_ns);
void busyWait(int wait_time_ns);

void setNextTriggerTime(struct timespec *next_trigger_time, Task_Info *task);
void checkResponseTime(Task_Info *task, int iteration_index, struct timespec global_start, struct timespec global_end);