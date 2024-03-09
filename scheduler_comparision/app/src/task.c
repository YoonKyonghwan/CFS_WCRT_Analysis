#include "task.h"
#include "util.h"

extern pthread_mutex_t mutex_memory_access;
extern pthread_barrier_t barrier;

void* phased_task_function(void* arg) {
    Task_Info *task = (Task_Info*)arg;

    pid_t tid = gettid();
    int sched_policy = sched_getscheduler(tid);
    printf(" phased_task_function: sched_policy : %d\n", sched_policy);

    struct sched_param param;
    int ret = sched_getparam(tid, &param);
    printf(" phased_task_function: priority : %d\n", param.sched_priority);

    // Wait for all threads to reach the barrier    
    pthread_barrier_wait(&barrier);

    struct timespec global_start, global_end, next_trigger_time;
    int iteration_index = 0;
    long long responsed_ns = 0;
    
    pthread_mutex_t period_mutex = PTHREAD_MUTEX_INITIALIZER;
    pthread_cond_t cond = PTHREAD_COND_INITIALIZER;

    pthread_mutex_lock(&period_mutex); // to control period

    while (1) {
        setNextTriggerTime(&next_trigger_time, task); // to control period
        clock_gettime(CLOCK_MONOTONIC, &global_start);
        
        PUSH_PROFILE("total")
        if (task->phased_read_time_ns > 0){
            PUSH_PROFILE("read")
            memoryAccess(task->phased_read_time_ns);
            POP_PROFILE()
        }

        PUSH_PROFILE("body")
        busyWait(task->phased_execution_time_ns[iteration_index]); // Todo
        POP_PROFILE()

        if (task->phased_write_time_ns > 0){
            PUSH_PROFILE("write")
            memoryAccess(task->phased_write_time_ns);
            POP_PROFILE()
        }

        POP_PROFILE() //for total
        clock_gettime(CLOCK_MONOTONIC, &global_end);

        responsed_ns = (global_end.tv_sec - global_start.tv_sec) * 1000000000LL + (global_end.tv_nsec - global_start.tv_nsec);
        task->response_time_ns[iteration_index] = responsed_ns;
        if (responsed_ns > task->wcrt_ns){
            task->wcrt_ns = responsed_ns;
        }
        
        iteration_index = (iteration_index + 1) % task->num_samples;
        pthread_cond_timedwait(&cond, &period_mutex, &next_trigger_time);
    }

    pthread_mutex_unlock(&period_mutex); // to control period

    return NULL;
}


void setNextTriggerTime(struct timespec *next_trigger_time, Task_Info *task){
    clock_gettime(CLOCK_REALTIME, next_trigger_time); // for managing period
    long long interarrival_time;
    if (task->isPeriodic ){
        interarrival_time = task->period_ns;
    }else{
        interarrival_time = task->low_interarrival_time_ns;
        interarrival_time += (rand() % (task->upper_interarrival_time_ns - task->low_interarrival_time_ns));
    }

    next_trigger_time->tv_sec += interarrival_time / 1000000000LL;
    next_trigger_time->tv_nsec += interarrival_time % 1000000000LL;
    if (next_trigger_time->tv_nsec >= 1000000000LL){
        next_trigger_time->tv_sec += 1;
        next_trigger_time->tv_nsec -= 1000000000LL;
    }
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

