#include "task.h"
#include "util.h"

void* task_function_unnifest(void* arg) {
    Task_Info *task = (Task_Info*)arg;

    // initialize variables
    PUSH_PROFILE("init")
    // pthread_mutex_t period_mutex = PTHREAD_MUTEX_INITIALIZER;
    // pthread_cond_t cond = PTHREAD_COND_INITIALIZER;
    int iteration_index = 0;
    long long interarrival_time = 0LL;
    long long real_wcet_ns = 0;
    long long real_execution_time=0;
    struct timespec start_execution_time, end_execution_time;
    setSchedPolicyPriority(task);
    // pthread_mutex_lock(&period_mutex); // to control period
    struct timespec current_trigger_time, job_end, next_trigger_time;
    current_trigger_time = global_start_time;
    next_trigger_time = current_trigger_time; //init
    printf(" (Init) %s \n", task->name);
    POP_PROFILE()

    // wait for all threads to be ready
    clock_nanosleep(CLOCK_MONOTONIC, TIMER_ABSTIME, &current_trigger_time, NULL);  //response time
    while (terminate == false) {
        clock_gettime(CLOCK_THREAD_CPUTIME_ID, &start_execution_time); //execution time
        PUSH_PROFILE(task->name) 
        busyWait(task->body_time_ns);
        POP_PROFILE() 
        clock_gettime(CLOCK_MONOTONIC, &job_end); //response time
        task->response_time_ns[iteration_index] = timeDiff(current_trigger_time, job_end);
        clock_gettime(CLOCK_THREAD_CPUTIME_ID, &end_execution_time); //execution time
        real_execution_time = timeDiff(start_execution_time, end_execution_time);
        if (real_execution_time > real_wcet_ns){
            real_wcet_ns = real_execution_time;
        }

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
    
    task->wcet_ns = real_wcet_ns;
    printf("%s(wcet %lld) task termintated \n", task->name, real_wcet_ns);
    return NULL;
}


void* non_RT_task_function(void* arg) {
    Task_Info *task = (Task_Info*)arg;

    // initialize variables
    PUSH_PROFILE("init")
    int iteration_index = 0;
    long long real_wcet_ns = 0;
    long long real_execution_time=0;
    struct timespec start_execution_time, end_execution_time;
    setSchedPolicyPriority(task);
    // pthread_mutex_lock(&period_mutex); // to control period
    struct timespec current_trigger_time, job_end;
    current_trigger_time = global_start_time;
    printf(" (Init) %s \n", task->name);
    POP_PROFILE()

    // wait for all threads to be ready
    clock_nanosleep(CLOCK_MONOTONIC, TIMER_ABSTIME, &current_trigger_time, NULL);  //response time
    // clock_gettime(CLOCK_MONOTONIC, &current_trigger_time); //response time
    while (terminate == false) {
        clock_gettime(CLOCK_THREAD_CPUTIME_ID, &start_execution_time); //execution time
        PUSH_PROFILE(task->name)
        busyWait(task->body_time_ns);
        POP_PROFILE() 
        clock_gettime(CLOCK_MONOTONIC, &job_end); //response time
        task->response_time_ns[iteration_index] = timeDiff(current_trigger_time, job_end);
        clock_gettime(CLOCK_THREAD_CPUTIME_ID, &end_execution_time); //execution time
        real_execution_time = timeDiff(start_execution_time, end_execution_time);
        if (real_execution_time > real_wcet_ns){
            real_wcet_ns = real_execution_time;
        }
        current_trigger_time = job_end;
        iteration_index = (iteration_index + 1) % task->num_samples;
    }
    
    task->wcet_ns = real_wcet_ns;
    printf("%s(wcet %lld) task termintated \n", task->name, real_wcet_ns);
    return NULL;
}

void setSchedPolicyPriority(Task_Info *task){
    // init variable
    pid_t tid = syscall(SYS_gettid);
    struct sched_attr attr;
    int ret;
    memset(&attr, 0, sizeof(attr));
    attr.size = sizeof(struct sched_attr);

    switch (task->sched_policy) {
        // Configurations in the thread function for CFS or EDF
        case CFS:
            ret = nice(task->nice_value);
            // printf("task_name : %s, nice value: %d, ret: %d\n", task->name, task->nice_value, ret);
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
            attr.sched_priority = sched_get_priority_max(SCHED_FIFO);
            break;
        case RR:
            //set sched_policyd
            attr.sched_policy = SCHED_RR;
            attr.sched_priority = sched_get_priority_max(SCHED_RR);
            break;
        case RM:
            //set sched_policy
            attr.sched_policy = SCHED_RR;
            attr.sched_priority = task->priority;
            break;
        default:
            printf("Check the supported scheduler type.\n");
            exit(1);
    }

    if (task->sched_policy == CFS){
        return;
    }else{
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


void runRunnable(int read_ns, int execution_ns, int write_ns){
    // if (read_ns > 0){
    //     PUSH_PROFILE("read")
    //     memoryAccess(read_ns);
    //     POP_PROFILE()
    // }

    PUSH_PROFILE("execution")
    // convert int to string
    char execution_ns_str[10] = "";
    sprintf(execution_ns_str, "%d", execution_ns);
    MARKER(execution_ns_str)
    busyWait(execution_ns);
    POP_PROFILE()

    // if (write_ns > 0){
    //     PUSH_PROFILE("write")
    //     memoryAccess(write_ns);
    //     POP_PROFILE()
    // }
    return;
}

 long long getInterarrivalTime(Task_Info *task, int iteration_index){
    if (task->isPeriodic ){
        return task->period_ns;
    }else{
        // interarrival_time = task->low_interarrival_time_ns;
        // interarrival_time += (rand() % (task->upper_interarrival_time_ns - task->low_interarrival_time_ns));
        return task->random_interarrival_time_ns[iteration_index];
    }
}


void setNextTriggerTime(struct timespec *next_trigger_time,  long long interarrival_time_ns){
    next_trigger_time->tv_sec += (interarrival_time_ns / 1000000000LL);
    next_trigger_time->tv_nsec += (interarrival_time_ns % 1000000000LL);
    if (next_trigger_time->tv_nsec >= 1000000000LL){
        next_trigger_time->tv_sec += 1;
        next_trigger_time->tv_nsec -= 1000000000LL;
    }
}

// make a function of time_end - time_start, the data type of time_end and time_start is struct timespec
long long timeDiff(struct timespec time_start, struct timespec time_end){
    return (time_end.tv_sec - time_start.tv_sec) * 1000000000LL + (time_end.tv_nsec - time_start.tv_nsec);
}


void memoryAccess(int time_ns) {
    pthread_mutex_lock(&mutex_memory_access);
    busyWait(time_ns);
    pthread_mutex_unlock(&mutex_memory_access);
}


// void busyWait(int wait_time_ns){
//     struct timespec start, end;
//     clock_gettime(CLOCK_THREAD_CPUTIME_ID, &start);
//      long long elapsed_ns = 0;
//     while (elapsed_ns < wait_time_ns) {
//         clock_gettime(CLOCK_THREAD_CPUTIME_ID, &end);
//         elapsed_ns = (end.tv_sec - start.tv_sec) * 1000000000LL + (end.tv_nsec - start.tv_nsec);
//     }
//     //for loop
// }

void busyWait( long long wait_time_ns){
    struct timespec start, end;
    clock_gettime(CLOCK_THREAD_CPUTIME_ID, &start);
    long long elapsed_ns = 0;
    while (elapsed_ns < wait_time_ns) {
        for (int i = 0; i < 1000; i++) {
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