#define _GNU_SOURCE
#pragma once
#include <pthread.h>
#include <time.h>
#include <unistd.h>
#include <sched.h>
#include <stdio.h>
#include <stdlib.h>
#include <signal.h>
#include <string.h>
#include <stdbool.h>
#include <sys/syscall.h> // for syscall(SYS_gettid)
#include <sys/resource.h> // for setpriority
#include <sys/mman.h> // for mlockall
#include <linux/types.h>
#include <limits.h>

#define USE_NVTX

#ifdef USE_NVTX
#include <nvtx3/nvToolsExt.h>
#define PUSH_PROFILE(name) nvtxRangePush(name);
#define POP_PROFILE() nvtxRangePop();
#define MARKER(name) nvtxMark(name);
#else
#define PUSH_PROFILE(name)
#define POP_PROFILE()
#define MARKER(name) 
#endif

#define min(x, y) (x) < (y) ? (x) : (y)

extern struct timespec global_start_time;
extern bool terminate;
// extern bool initial_try;

typedef struct {
    char *name;
    char core_index;
    char sched_policy;
    int priority;
    int nice_value;
    bool isRTTask;

    bool isPeriodic;
    long long period_ns;
    long long low_interarrival_time_ns;
    long long upper_interarrival_time_ns;

    long long body_time_ns;

    int num_samples;

    long long wcet_ns;
    long long real_wcet_ns;
    long long *response_time_ns;
    long long wcrt_ns;
    long long *start_time_ns;
    long long *end_time_ns;
    
} Task_Info;


struct sched_attr {
	__u32 size;

	__u32 sched_policy;
	__u64 sched_flags;

	/* SCHED_NORMAL, SCHED_BATCH */
	__s32 sched_nice;

	/* SCHED_FIFO, SCHED_RR */
	__u32 sched_priority;

	/* SCHED_DEADLINE (nsec) */
	__u64 sched_runtime;
	__u64 sched_deadline;
	__u64 sched_period;
};


void* task_function(void* arg);

void LockMemory();
void busyWait( long long wait_time_ns);

void setSchedPolicyPriority(Task_Info *task);
int sched_setattr(pid_t pid, const struct sched_attr *attr,  int flags);

void setNextTriggerTime(struct timespec *next_trigger_time, long long interarrival_time_ns);
void addNanoSecondToTimespec(struct timespec *time_info, long long add_time_ns);
long long timeDiff(struct timespec time_start, struct timespec time_end);
