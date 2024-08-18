#include "task.h"
#include "util.h"


void* task_function(void* arg) {
    Task_Info *task = (Task_Info*)arg;

    PUSH_PROFILE("Init")
    int iteration_index = 0;
    long long real_execution_time=0;
    long long response_time_ns = 0;
    struct timespec current_trigger_time, job_end, next_trigger_time; // for period, response time
    struct timespec start_execution_time, end_execution_time; // for execution time
    setSchedPolicyPriority(task);
    current_trigger_time = global_start_time;
    next_trigger_time = current_trigger_time; //init
    if (!initial_try){ 
        // it initial try, all tasks release at the same time
        // if not initial try, add random offset to the trigger time of each task
        // int random_offset = rand() % (task->period_ns / 2);
        // addNanoSecondToTimespec(&current_trigger_time, random_offset);
        // addNanoSecondToTimespec(&next_trigger_time, random_offset);
    }
    printf("     (Init) %s \n", task->name);
    POP_PROFILE()

    // wait for all threads to be ready
    clock_nanosleep(CLOCK_MONOTONIC, TIMER_ABSTIME, &current_trigger_time, NULL);  //response time

    // iterative execution
    while (terminate == false) {
        PUSH_PROFILE(task->name) 
        clock_gettime(CLOCK_THREAD_CPUTIME_ID, &start_execution_time); //execution time
        busyWait(task->body_time_ns);
        clock_gettime(CLOCK_MONOTONIC, &job_end); //response time
        response_time_ns = timeDiff(current_trigger_time, job_end);
        task->response_time_ns[iteration_index] = response_time_ns;
        if (task->wcrt_ns < response_time_ns && iteration_index != 0){
            task->wcrt_ns = response_time_ns;
        }
        clock_gettime(CLOCK_THREAD_CPUTIME_ID, &end_execution_time); //execution time
        real_execution_time = timeDiff(start_execution_time, end_execution_time);
        if (real_execution_time > task->real_wcet_ns && iteration_index != 0){
            task->real_wcet_ns = real_execution_time;
        }
        POP_PROFILE() 
        // printf("%s(rt %lld, et %lld) \n", task->name, response_time_ns, real_execution_time);

        if (task->period_ns > task->response_time_ns[iteration_index]) {
            setNextTriggerTime(&next_trigger_time, task->period_ns);
            current_trigger_time = next_trigger_time;
            clock_nanosleep(CLOCK_MONOTONIC, TIMER_ABSTIME, &next_trigger_time, NULL);
        }else{
            clock_gettime(CLOCK_MONOTONIC, &next_trigger_time);
            current_trigger_time = next_trigger_time;
        }
        iteration_index = (iteration_index + 1) % task->num_samples;
    }
    
    printf("     %s(wcrt %lld, wcet %lld) termintated \n", task->name, task->wcrt_ns, task->real_wcet_ns);
    return NULL;
}

void setSchedPolicyPriority(Task_Info *task){
    pid_t tid = syscall(SYS_gettid);
    struct sched_attr attr;
    int ret;
    memset(&attr, 0, sizeof(attr));
    attr.size = sizeof(struct sched_attr);

    switch (task->sched_policy) {
        case CFS:
            ret = nice(task->nice_value);
            break;
        case EDF:
            attr.sched_policy = SCHED_DEADLINE;
            if (task->isPeriodic){
                attr.sched_deadline = task->period_ns; //ns
                attr.sched_period = task->period_ns; //ns
            }else{
                attr.sched_deadline = task->low_interarrival_time_ns; //ns
                attr.sched_period = task->low_interarrival_time_ns; //ns
            }
            int margin = min(400 * 1000, (int) attr.sched_period /10); // 400us
            attr.sched_runtime = task->wcet_ns + margin;  //ns
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
    return;
}


int sched_setattr(pid_t pid, const struct sched_attr *attr,  int flags) {
	return syscall(__NR_sched_setattr, pid, attr, flags);
}


void checkResponseTime(Task_Info *task, int iteration_index, struct timespec start_time, struct timespec end_time){
     long long responsed_ns = (end_time.tv_sec - start_time.tv_sec) * 1000000000LL + (end_time.tv_nsec - start_time.tv_nsec);
    if (iteration_index != 0){
        task->response_time_ns[iteration_index] = responsed_ns;
        // task->start_time_ns[iteration_index] = (start_time.tv_sec * 1000000000LL ) + start_time.tv_nsec;
        // task->end_time_ns[iteration_index] = (end_time.tv_sec * 1000000000LL ) + end_time.tv_nsec;
    }
    return;
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

void busyWait( long long wait_time_ns){
    struct timespec start, end;
    clock_gettime(CLOCK_THREAD_CPUTIME_ID, &start);
    long long elapsed_ns = 0;
    while (elapsed_ns < wait_time_ns) {
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