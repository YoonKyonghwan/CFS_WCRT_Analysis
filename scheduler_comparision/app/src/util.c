#include "util.h"

long long min_period_ns = LLONG_MAX;

void setTaskInfo_fmtv(json_object *jobj, Task_Info *task){
    task->name = json_object_get_string(json_object_object_get(jobj, "task_name"));
    task->core_index = json_object_get_int(json_object_object_get(jobj, "core_index"));
    task->priority = json_object_get_int(json_object_object_get(jobj, "priority"));
    task->isRTTask = json_object_get_boolean(json_object_object_get(jobj, "isRTTask"));

    task->isPeriodic = json_object_get_boolean(json_object_object_get(jobj, "isPeriodic"));
    task->period_ns = json_object_get_int64(json_object_object_get(jobj, "period_ns"));
    task->low_interarrival_time_ns = json_object_get_int64(json_object_object_get(jobj, "lower_bound_ns"));
    task->upper_interarrival_time_ns = json_object_get_int64(json_object_object_get(jobj, "upper_bound_ns"));


    task->num_samples = json_object_array_length(json_object_object_get(jobj, "phased_execution"));
    // task->num_runnables = json_object_array_length(json_object_object_get(jobj, "time_ns"));
    
    if (task->isPeriodic){
        if (task->period_ns < min_period_ns){
            min_period_ns = task->period_ns;
        }
        task->random_interarrival_time_ns = NULL;
    }else{
        if (task->low_interarrival_time_ns < min_period_ns){
            min_period_ns = task->low_interarrival_time_ns;
        }
        task->random_interarrival_time_ns = (long long*)malloc(task->num_samples * sizeof(long long));
        json_object *inter_arrival_time_ns = json_object_object_get(jobj, "inter_arrival_time_ns");
        for (int i = 0; i < task->num_samples; i++){
            task->random_interarrival_time_ns[i] = json_object_get_int64(json_object_array_get_idx(inter_arrival_time_ns, i));
        }
    }

    task->phased_read_time_ns = json_object_get_int64(json_object_object_get(jobj, "phased_read"));
    task->phased_write_time_ns = json_object_get_int64(json_object_object_get(jobj, "phased_write"));
    task->phased_execution_time_ns = (long long*)malloc(task->num_samples * sizeof(long long));
    json_object *phased_execution = json_object_object_get(jobj, "phased_execution");
    int max_execution = 0;
    for (int i = 0; i < task->num_samples; i++){
        task->phased_execution_time_ns[i] = json_object_get_int64(json_object_array_get_idx(phased_execution, i));
        if (task->phased_execution_time_ns[i] > max_execution){
            max_execution = task->phased_execution_time_ns[i];
        }
    }

    // task->runnables_read_time_ns = (int*)malloc(task->num_runnables * sizeof(int));
    // task->runnables_write_time_ns = (int*)malloc(task->num_runnables * sizeof(int));
    // task->runnables_execution_time_ns = (int**)malloc(task->num_runnables * sizeof(int*));
    // json_object *runnables_info = json_object_object_get(jobj, "time_ns");
    // for (int i = 0; i < task->num_runnables; i++){
    //     json_object *runnable = json_object_array_get_idx(runnables_info, i);
    //     task->runnables_read_time_ns[i] = json_object_get_int(json_object_object_get(runnable, "read"));
    //     task->runnables_write_time_ns[i] = json_object_get_int(json_object_object_get(runnable, "write"));
    //     task->runnables_execution_time_ns[i] = (int*)malloc(task->num_samples * sizeof(int));
    //     json_object *execution = json_object_object_get(runnable, "execution");
    //     for (int j = 0; j < task->num_samples; j++){
    //         task->runnables_execution_time_ns[i][j] = json_object_get_int(json_object_array_get_idx(execution, j));
    //     }
    // }

    task->wcet_ns = max_execution;
    // task->wcet_ns = task->phased_read_time_ns + max_execution + task->phased_write_time_ns;
    task->response_time_ns = ( long long*)malloc(task->num_samples * sizeof( long long));
    // task->start_time_ns = (long long*)malloc(task->num_samples * sizeof(long long));
    // task->end_time_ns = (long long*)malloc(task->num_samples * sizeof(long long));
    for (int i = 0; i < task->num_samples; i++){
        task->response_time_ns[i] = 0;
        // task->start_time_ns[i] = 0;
        // task->end_time_ns[i] = 0;
    }
    task->wcrt_ns = 0;

    return;
}

int setNiceValueByDeadline( long long period_ns,  long long min_period_ns, double nice_lambda){
    double relative_weight = log((double)period_ns/(double)min_period_ns) / log(1.25);
    relative_weight *= nice_lambda;
    return min(-20 + ceil(relative_weight), 19);
}

void updateRealWCET(char* input_file_name, Task_Info *tasks, int num_tasks){
    json_object *org_info = json_object_from_file(input_file_name);
    json_object *tasks_ids = json_object_object_get(org_info, "idNameMap");
    json_object *mapping_info = json_object_object_get(org_info, "mappingInfo");
    int num_cores = json_object_array_length(mapping_info);
    char *task_id_str = (char*)malloc(3);
    
    for (int i = 0; i < num_cores; i++){
        json_object *core_info = json_object_array_get_idx(mapping_info, i);
        json_object *tasks_info = json_object_object_get(core_info, "tasks");
        int num_tasks_core = json_object_array_length(tasks_info);
        for (int j = 0; j < num_tasks_core; j++){
            json_object *task_info = json_object_array_get_idx(tasks_info, j);
            int task_id = json_object_get_int(json_object_object_get(task_info, "id"));
            sprintf(task_id_str, "%d", task_id);
            char *task_name = json_object_get_string(json_object_object_get(tasks_ids, task_id_str));
            long long real_wcet = getWCETByName(task_name, tasks, num_tasks)/1000 + 1;
            //  long long wcet = json_object_get_int64(json_object_object_get(task_info, "bodyTime"));
            // printf("Task name: %s, real_wcet: %d, wcet: %d\n", task_name, real_wcet, wcet);
            //update real_wcet
            json_object_set_int(json_object_object_get(task_info, "bodyTime"), real_wcet);
        }
    }

    //remove the original file
    remove(input_file_name);
    json_object_to_file_ext(input_file_name, org_info, JSON_C_TO_STRING_PRETTY);
    free(task_id_str);

    return;
}

 long long getWCETByName(char* task, Task_Info *tasks, int num_tasks){
    for (int i = 0; i < num_tasks; i++){
        if (strcmp(task, tasks[i].name) == 0){
            return tasks[i].wcet_ns;
        }
    }
    return -1;
}


void saveResultToJson(int num_tasks, Task_Info *tasks, Task_Info *non_RT_task, char *result_file_name){
    json_object *tasks_result = json_object_new_array();
    for (int i = 0; i < num_tasks; i++) {
        json_object *task_result = json_object_new_object();
        convertTaskResultToJson(task_result, &tasks[i]);
        json_object_array_add(tasks_result, task_result);
    }
    if (non_RT_task != NULL){
        json_object *task_result = json_object_new_object();
        convertTaskResultToJson(task_result, non_RT_task);
        json_object_array_add(tasks_result, task_result);
    }
    json_object_to_file_ext(result_file_name, tasks_result, JSON_C_TO_STRING_PRETTY);

    return;
}

void convertTaskResultToJson(json_object *task_result, Task_Info *task){
    json_object *task_response_time_ns = json_object_new_array();
    json_object *task_start_time_ns = json_object_new_array();
    json_object *task_end_time_ns = json_object_new_array();
    long long total_response_time_ns = 0;
    long long wcrt_ns = 0;
    int count_valid_response_time = 0;
    for (int j = 1; j < task->num_samples; j++) {
        if (task->response_time_ns[j] != 0){
            json_object_array_add(task_response_time_ns, json_object_new_int64(task->response_time_ns[j]));
            total_response_time_ns += task->response_time_ns[j];
            count_valid_response_time++;
            // json_object_array_add(task_start_time_ns, json_object_new_int64(tasks[i].start_time_ns[j]));
            // json_object_array_add(task_end_time_ns, json_object_new_int64(tasks[i].end_time_ns[j]));
            if (task->response_time_ns[j] > wcrt_ns){
                wcrt_ns = task->response_time_ns[j];
            }
        }
    }
    long long avg_response_time_ns = 0;
    if (count_valid_response_time != 0){
        avg_response_time_ns= total_response_time_ns / count_valid_response_time;
    }
    task->wcrt_ns = wcrt_ns;

    long long deadline_ns = 0;
    if (task->isRTTask){
        if (task->isPeriodic){
            deadline_ns = task->period_ns;
        }else{
            deadline_ns = task->low_interarrival_time_ns;
        }
    }else{
        deadline_ns = 100*1000*1000; // 100ms, not important
    }
    json_object_object_add(task_result, "task_name", json_object_new_string(task->name));
    json_object_object_add(task_result, "core_index", json_object_new_int(task->core_index));
    json_object_object_add(task_result, "nice_value", json_object_new_int(task->nice_value));
    json_object_object_add(task_result, "deadline_ns", json_object_new_int64(deadline_ns));
    json_object_object_add(task_result, "wcrt_ns", json_object_new_int64(task->wcrt_ns));
    json_object_object_add(task_result, "wcet_ns", json_object_new_int64(task->wcet_ns));
    json_object_object_add(task_result, "avg_response_time_ns", json_object_new_int64(avg_response_time_ns));
    json_object_object_add(task_result, "response_time_ns", task_response_time_ns);
    // json_object_object_add(task_result, "start_time_ns", task_start_time_ns);
    // json_object_object_add(task_result, "end_time_ns", task_end_time_ns);
}

void freeTaskInfo(Task_Info *task){
    free(task->phased_execution_time_ns);
    // free(task->runnables_read_time_ns);
    // free(task->runnables_write_time_ns);
    free(task->response_time_ns);
    // for (int i = 0; i < task->num_runnables; i++){
    //     free(task->runnables_execution_time_ns[i]);
    // }
    // free(task->runnables_execution_time_ns);
    return;
}


void setCoreMapping(pthread_attr_t *threadAttr, Task_Info *task){
    // initialize thread attribute
    if (pthread_attr_init(threadAttr)){
        printf("Fail to initialize thread attribute.\n");
        exit(1);
    }

    // set schedule policy and priority
    // setSchedPolicyPriority(threadAttr, task);

    cpu_set_t cpuset;
    CPU_ZERO(&cpuset);
    CPU_SET(task->core_index, &cpuset);
    if (pthread_attr_setaffinity_np(threadAttr, sizeof(cpu_set_t), &cpuset) != 0){
        perror("pthread_attr_setaffinity_np");
        exit(1);
    }

    if (pthread_attr_setinheritsched(threadAttr, PTHREAD_EXPLICIT_SCHED)){
        printf("Fail to set inherit scheduler attribute.\n");
        exit(1);
    }
    
    return;
}



// void setSchedPolicyPriority(pthread_attr_t *threadAttr, Task_Info *task){
//     struct sched_param schedparam;
//     switch (task->sched_policy) {
//         // Configurations in the thread function for CFS or EDF
//         case CFS:
//             break;
//         case EDF:
//             break;
//         case FIFO: 
//             //set sched_policy
//             if (pthread_attr_setschedpolicy(threadAttr, SCHED_FIFO)){
//                 printf("Fail to set schedule policy.\n");
//                 exit(1);
//             }
//             //set priority
//             schedparam.sched_priority = sched_get_priority_max(SCHED_FIFO);
//             if (pthread_attr_setschedparam(threadAttr, &schedparam)){
//                 printf("Fail to set scheduling priority.\n");
//                 exit(1);
//             }
//             break;
//         case RR:
//             //set sched_policy
//             if (pthread_attr_setschedpolicy(threadAttr, SCHED_RR)){
//                 printf("Fail to set schedule policy.\n");
//                 exit(1);
//             }
//             //set priority
//             schedparam.sched_priority = sched_get_priority_max(SCHED_RR);
//             if (pthread_attr_setschedparam(threadAttr, &schedparam)){
//                 printf("Fail to set scheduling priority.\n");
//                 exit(1);
//             }
//             break;
//         case RM:
//             //set sched_policy
//             if (pthread_attr_setschedpolicy(threadAttr, SCHED_FIFO)){
//                 printf("Fail to set schedule policy.\n");
//                 exit(1);
//             }
//             //set priority
//             schedparam.sched_priority = task->priority;
//             if (pthread_attr_setschedparam(threadAttr, &schedparam)){
//                 printf("Fail to set scheduling priority.\n");
//                 exit(1);
//             }
//             break;
//         default:
//             printf("Check the supported scheduler type.\n");
//             exit(1);
//     }
//     return;
// }

void printSchedPolicy(int policy){
    switch (policy){
        case CFS:
            printf("Scheduling policy : CFS\n");
            break;
        case FIFO:
            printf("Scheduling policy : FIFO\n");
            break;
        case RR:
            printf("Scheduling policy : RR\n");
            break;
        case EDF:
            printf("Scheduling policy : EDF\n");
            break;
        case RM:
            printf("Scheduling policy : RM\n");
            break;
        default:
            printf("Check the supported scheduler type.\n");
            break;
    }
    return;
}

void initMutex(pthread_mutex_t *mutex_memory_access, int mutex_protocol){
    pthread_mutexattr_t mutexAttr;
    if (pthread_mutexattr_init(&mutexAttr))
    {
        printf("Fail to initialize mutex attribute\n");
        exit(1);
    }
    if (pthread_mutexattr_setprotocol(&mutexAttr, mutex_protocol))
    {
        printf("Fail to set mutex protocol\n");
        exit(1);
    }
    if (pthread_mutex_init(mutex_memory_access, &mutexAttr))
    {
        printf("Fail to initialize mutex\n");
        exit(1);
    }
}