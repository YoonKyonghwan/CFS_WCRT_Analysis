#include "task.h"
#include "util.h"


void* task_function(void* arg) {
    Task_Info *task = (Task_Info*)arg;

    // initialize variables
    PUSH_PROFILE("init")
    pthread_mutex_t period_mutex = PTHREAD_MUTEX_INITIALIZER;
    pthread_cond_t cond = PTHREAD_COND_INITIALIZER;
    int iteration_index = 0;
    struct timespec current_trigger_time, global_end, next_trigger_time;
    long long sleep_time = 0LL;
    long long interarrival_time = 0LL;

    printf(" (Init) %s \n", task->name);
    pthread_mutex_lock(&period_mutex); // to control period
    POP_PROFILE()

    // Wait for all threads to reach the barrier    
    pthread_barrier_wait(&barrier);
    setSchedPolicyPriority(task);
    // sleep 0.1 sec to make sure that all threads are ready to start
    struct timespec init_sleep_time = {0, 10000000}; // 10 ms
    nanosleep(&init_sleep_time, NULL);
    MARKER("after barrier")

    current_trigger_time = global_start_time;
    next_trigger_time = global_start_time;

    while (terminate == false) {
        PUSH_PROFILE(task->name) // for total(read + execution + write)
        if (isPhasedTask){
            runRunnable(task->phased_read_time_ns, task->phased_execution_time_ns[iteration_index], task->phased_write_time_ns);
        }else{
            for (int i = 0; i < task->num_runnables; i++){
                runRunnable(task->runnables_read_time_ns[i], task->runnables_execution_time_ns[i][iteration_index], task->runnables_write_time_ns[i]);
            }
        }

        clock_gettime(CLOCK_REALTIME, &global_end);
        POP_PROFILE() // for total(read + execution + write)
        checkResponseTime(task, iteration_index, current_trigger_time, global_end);

        interarrival_time = getInterarrivalTime(task, iteration_index);
        if (interarrival_time > task->response_time_ns[iteration_index]) {
            setNextTriggerTime(&next_trigger_time, interarrival_time);
            pthread_cond_timedwait(&cond, &period_mutex, &next_trigger_time); //wait until next_trigger_time
        }else{
            clock_gettime(CLOCK_REALTIME, &next_trigger_time);
        }
        current_trigger_time = next_trigger_time;
        iteration_index = (iteration_index + 1) % task->num_samples;
    }
    pthread_mutex_unlock(&period_mutex); // to control period

    printf("%s task termintated\n", task->name);
    return NULL;
}

// void* task_function(void* arg) {
//     Task_Info *task = (Task_Info*)arg;

//     // initialize variables
//     PUSH_PROFILE("init")
//     printf(" (Init) %s \n", task->name);
//     int iteration_index = 0;
//     struct timespec current_trigger_time, global_end, next_trigger_time;
//     struct timespec sleep_time;
//     long long sleep_time_ns = 0LL;  
//     long long interarrival_time = 0LL;
//     POP_PROFILE()

//     // Wait for all threads to reach the barrier    
//     pthread_barrier_wait(&barrier);
//     setSchedPolicyPriority(task);
//     MARKER("after barrier")

//     current_trigger_time = global_start_time;
//     next_trigger_time = global_start_time;

//     while (terminate == false) {
//         PUSH_PROFILE(task->name) // for total(read + execution + write)
//         if (isPhasedTask){
//             runRunnable(task->phased_read_time_ns, task->phased_execution_time_ns[iteration_index], task->phased_write_time_ns);
//         }else{
//             for (int i = 0; i < task->num_runnables; i++){
//                 runRunnable(task->runnables_read_time_ns[i], task->runnables_execution_time_ns[i][iteration_index], task->runnables_write_time_ns[i]);
//             }
//         }

//         clock_gettime(CLOCK_REALTIME, &global_end);
//         POP_PROFILE() // for total(read + execution + write)
//         checkResponseTime(task, iteration_index, current_trigger_time, global_end);

//         interarrival_time = getInterarrivalTime(task, iteration_index);
//         sleep_time_ns = interarrival_time - task->response_time_ns[iteration_index];
//         if (sleep_time_ns > 0){
//             setNextTriggerTime(&next_trigger_time, interarrival_time);
//             convert_nsTime_timespec(sleep_time_ns, &sleep_time);
//             nanosleep(&sleep_time, NULL);
//         }else{
//             clock_gettime(CLOCK_REALTIME, &next_trigger_time);
//         }
//         current_trigger_time = next_trigger_time;
//         iteration_index = (iteration_index + 1) % task->num_samples;

//     }

//     printf("%s task termintated\n", task->name);
//     return NULL;
// }


void convert_nsTime_timespec(long long nsTime, struct timespec *time){
    time->tv_sec = nsTime / 1000000000LL;
    time->tv_nsec = nsTime % 1000000000LL;
    return;
}


void setSchedPolicyPriority(Task_Info *task){
    // init variable
    pid_t tid = syscall(SYS_gettid);
    struct sched_attr attr;
    memset(&attr, 0, sizeof(attr));
    attr.size = sizeof(struct sched_attr);

    switch (task->sched_policy) {
        // Configurations in the thread function for CFS or EDF
        case CFS:
            setpriority(PRIO_PROCESS, syscall(SYS_gettid), task->nice_value);
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
            //set sched_policy
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


int sched_setattr(pid_t pid, const struct sched_attr *attr, unsigned int flags) {
	return syscall(__NR_sched_setattr, pid, attr, flags);
}


void checkResponseTime(Task_Info *task, int iteration_index, struct timespec start_time, struct timespec end_time){
    long long responsed_ns = (end_time.tv_sec - start_time.tv_sec) * 1000000000LL + (end_time.tv_nsec - start_time.tv_nsec);
    if (iteration_index == 0){
        responsed_ns -= 10000000; // 10ms for initial sleep
    }
    task->response_time_ns[iteration_index] = responsed_ns;
    // task->start_time_ns[iteration_index] = (start_time.tv_sec * 1000000000LL ) + start_time.tv_nsec;
    // task->end_time_ns[iteration_index] = (end_time.tv_sec * 1000000000LL ) + end_time.tv_nsec;
    if (responsed_ns > task->wcrt_ns){
        task->wcrt_ns = responsed_ns;
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


void setNextTriggerTime(struct timespec *next_trigger_time, long long interarrival_time_ns){
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


void busyWait(int wait_time_ns){
    struct timespec start, end;
    clock_gettime(CLOCK_THREAD_CPUTIME_ID, &start);
    long long elapsed_ns;
    while (1) {
        clock_gettime(CLOCK_THREAD_CPUTIME_ID, &end);
        elapsed_ns = (end.tv_sec - start.tv_sec) * 1000000000LL + (end.tv_nsec - start.tv_nsec);
        if (elapsed_ns >= wait_time_ns) {
            break;
        }
    }
}

