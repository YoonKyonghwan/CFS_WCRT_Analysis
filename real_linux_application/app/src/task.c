#include "task.h"
#include "util.h"


void* task_function(void* arg) {
    Task_Info *task = (Task_Info*)arg;

    PUSH_PROFILE("Init")
    int iteration_index = 0;
    long long real_execution_time=0;
    long long response_time_ns = 0;
    struct timespec trigger_time, job_end; // for period, response time
    struct timespec start_execution_time, end_execution_time; // for execution time
    setSchedPolicyPriority(task);
    trigger_time = global_start_time;
    int random_offset = (rand() % 100) * 1000; // 0 ~ 100us
    addNanoSecondToTimespec(&trigger_time, random_offset);
    printf("     (Init) %s \n", task->name);
    POP_PROFILE()

    // wait for all threads to be ready
    clock_nanosleep(CLOCK_MONOTONIC, TIMER_ABSTIME, &trigger_time, NULL);  //response time
    usleep(1000); // 1ms
    // iterative execution
    while (terminate == false) {
        PUSH_PROFILE(task->name) 
        clock_gettime(CLOCK_THREAD_CPUTIME_ID, &start_execution_time); //execution time
        busyWait(task->body_time_ns);
        clock_gettime(CLOCK_MONOTONIC, &job_end); //response time
        response_time_ns = timeDiff(trigger_time, job_end);
        task->response_time_ns[iteration_index] = response_time_ns;
        // task->start_time_ns[iteration_index] = (trigger_time.tv_sec * 1000000000LL ) + trigger_time.tv_nsec;
        // task->end_time_ns[iteration_index] = (job_end.tv_sec * 1000000000LL ) + job_end.tv_nsec;
        if (task->wcrt_ns < response_time_ns && iteration_index != 0 ){
            task->wcrt_ns = response_time_ns;
        }
        clock_gettime(CLOCK_THREAD_CPUTIME_ID, &end_execution_time); //execution time
        real_execution_time = timeDiff(start_execution_time, end_execution_time);
        if (real_execution_time > task->real_wcet_ns && iteration_index != 0 ){
            task->real_wcet_ns = real_execution_time;
        }
        // printf("%s(rt %lld, et %lld) \n", task->name, response_time_ns, real_execution_time);

        if (task->period_ns > task->response_time_ns[iteration_index]) {
            setNextTriggerTime(&trigger_time, task->period_ns);
            POP_PROFILE() 
            clock_nanosleep(CLOCK_MONOTONIC, TIMER_ABSTIME, &trigger_time, NULL);
        }else{
            clock_gettime(CLOCK_MONOTONIC, &trigger_time);
            POP_PROFILE() 
        }
        iteration_index = (iteration_index + 1) % task->num_samples;
    }
    
    printf("     %s(period %lld, wcrt %lld, wcet %lld) termintated \n", task->name, task->period_ns, task->wcrt_ns, task->real_wcet_ns);
    return NULL;
}

void setSchedPolicyPriority(Task_Info *task){
    pid_t tid = syscall(SYS_gettid);
    struct sched_attr attr;
    int ret;
    memset(&attr, 0, sizeof(attr));
    attr.size = sizeof(struct sched_attr);

    if (task->isRTTask == false){
        ret = nice(task->nice_value);
    }else{
        switch (task->sched_policy) {
            case CFS:
                ret = nice(task->nice_value);
                break;
            case EDF:
                attr.sched_policy = SCHED_DEADLINE;
                __u64 max_period = 3000000000LL;
                if (task->isPeriodic){
                    attr.sched_deadline = min(task->period_ns, max_period); //ns
                    attr.sched_period = min(task->period_ns, max_period); //ns
                }else{
                    attr.sched_deadline = min(task->low_interarrival_time_ns, max_period); //ns
                    attr.sched_period = min(task->low_interarrival_time_ns, max_period); //ns
                }
                long long runtime = task->wcet_ns + task->wcet_ns/20;  //ns (105% of wcet)
                // round up to the nearest multiple of 1000000
                runtime = ((runtime + 999999) / 1000000) * 1000000;
                attr.sched_runtime = runtime;  //ns (105% of wcet)
                break;
            case FIFO: 
                attr.sched_policy = SCHED_FIFO;
                attr.sched_priority = task->priority;
                break;
            case RR:
                //set sched_policyd
                attr.sched_policy = SCHED_RR;
                attr.sched_priority = task->priority;
                break;
            default:
                printf("Check the supported scheduler type.\n");
                exit(1);
        }

        if (task->sched_policy != CFS){
            if (sched_setattr(tid, &attr, 0) < 0){
                perror("sched_setattr");
                exit(1);
            }
        }
    }
    return;
}


int sched_setattr(pid_t pid, const struct sched_attr *attr,  int flags) {
	return syscall(__NR_sched_setattr, pid, attr, flags);
}


void setNextTriggerTime(struct timespec *next_trigger_time,  long long interarrival_time_ns){
    addNanoSecondToTimespec(next_trigger_time, interarrival_time_ns);
}

void addNanoSecondToTimespec(struct timespec *time_info, long long add_time_ns){
    time_info->tv_sec += (add_time_ns / 1000000000LL);
    time_info->tv_nsec += (add_time_ns % 1000000000LL);
    if (time_info->tv_nsec >= 1000000000LL)
    {
        time_info->tv_sec += 1;
        time_info->tv_nsec -= 1000000000LL;
    }
}

long long timeDiff(struct timespec time_start, struct timespec time_end){
    return (time_end.tv_sec - time_start.tv_sec) * 1000000000LL + (time_end.tv_nsec - time_start.tv_nsec);
}

void busyWait( long long busy_time_ns){
    struct timespec start, end;
    clock_gettime(CLOCK_THREAD_CPUTIME_ID, &start);
    long long elapsed_ns = 0;
    while (elapsed_ns < busy_time_ns - 100000) { // 100us margin
        for (int i = 0; i < 100; i++) {
            //waste time
        }
        clock_gettime(CLOCK_THREAD_CPUTIME_ID, &end);
        elapsed_ns = (end.tv_sec - start.tv_sec) * 1000000000LL + (end.tv_nsec - start.tv_nsec);
    }
    return;
}

void LockMemory() {
    int ret = mlockall(MCL_CURRENT | MCL_FUTURE);
    if (ret != 0) {
        printf("mlockall failed\n");
        exit(1);
    }
}