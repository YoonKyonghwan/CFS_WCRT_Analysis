#include "util.h"

long long min_period_ns = LLONG_MAX;

void setTaskInfo(char *json_file_name, Task_Info *tasks, int sched_policy){
    json_object *tasks_info_json = json_object_from_file(json_file_name);
    if (tasks_info_json == NULL){
        printf("Error: when read json file\n");
        exit(1);
    }

    json_object *tasks_ids = json_object_object_get(tasks_info_json, "idNameMap");
    json_object *mapping_info = json_object_object_get(tasks_info_json, "mappingInfo");

    int num_cores = json_object_array_length(mapping_info);
    int num_tasks = getNumTasks(json_file_name);

    int task_index = 0;
    for (int i = 0; i < num_cores; i++){
        json_object *core_info = json_object_array_get_idx(mapping_info, i);
        int core_id = json_object_get_int(json_object_object_get(core_info, "coreID"));
        json_object *tasks_info = json_object_object_get(core_info, "tasks");
        int num_tasks_core = json_object_array_length(tasks_info);
        for (int j = 0; j < num_tasks_core; j++){
            json_object *task_info = json_object_array_get_idx(tasks_info, j);
            int task_id = json_object_get_int(json_object_object_get(task_info, "id"));
            char *task_id_str = (char*)malloc(3);
            sprintf(task_id_str, "%d", task_id);       
            tasks[task_index].name = json_object_get_string(json_object_object_get(tasks_ids, task_id_str));
            tasks[task_index].core_index = core_id;
            tasks[task_index].isRTTask = true;
            tasks[task_index].sched_policy = sched_policy; // atoi(argv[1]);
            tasks[task_index].isPeriodic = true;
            tasks[task_index].nice_value = json_object_get_int(json_object_object_get(task_info, "nice"));
            tasks[task_index].period_ns = 1000 * json_object_get_int64(json_object_object_get(task_info, "period"));
            tasks[task_index].body_time_ns = 1000 * json_object_get_int64(json_object_object_get(task_info, "bodyTime"));
            tasks[task_index].wcet_ns = tasks[task_index].body_time_ns;
            tasks[task_index].real_wcet_ns = tasks[task_index].wcet_ns; // initial value
            tasks[task_index].wcrt_ns = 0;
            task_index++;
        }
    }

    // compute hyperperiod of tasks
    long long hyperperiod_ns = getHyperperiod_ns(tasks, num_tasks);

    for (int i = 0; i < num_tasks; i++){
        tasks[i].num_samples = (2 * hyperperiod_ns) / tasks[i].period_ns; //us
        tasks[i].response_time_ns = (long long*)malloc(tasks[i].num_samples * sizeof(long long));
        tasks[i].start_time_ns = (long long*)malloc(tasks[i].num_samples * sizeof(long long));
        tasks[i].end_time_ns = (long long*)malloc(tasks[i].num_samples * sizeof(long long));
        for (int k = 0; k < tasks[i].num_samples; k++){
            tasks[i].response_time_ns[k] = 0;
            tasks[i].start_time_ns[k] = 0;
            tasks[i].end_time_ns[k] = 0;
        }
    }
    
    return;
}

long long getHyperperiod_ns(Task_Info *tasks, int num_tasks){
    long long hyperperiod_ns = 1;
    for (int i = 0; i < num_tasks; i++){
        hyperperiod_ns = lcm(hyperperiod_ns, tasks[i].period_ns);
    }
    return hyperperiod_ns;
}

long long gcd(long long a, long long b) {
	if (a == 0) return b;
	return gcd(b % a, a);
}

long long lcm(long long a, long long b) {
	return (a * b) / gcd(a, b);
}



void setNonRTTaskInfo(Task_Info* non_rt_task, char* name, int core_index, int execution_ns, int period_ns, int num_samples){
    non_rt_task->name = name; "Non_RT_Task";
    non_rt_task->isRTTask = false;
    non_rt_task->sched_policy = CFS;
    non_rt_task->isPeriodic = false;
    non_rt_task->nice_value = 19;

    non_rt_task->core_index = core_index;
    non_rt_task->period_ns = period_ns;
    non_rt_task->body_time_ns = execution_ns;
    non_rt_task->num_samples = num_samples; // simulation_period_sec * 10; //max: 10 per 1sec
    
    non_rt_task->wcet_ns = execution_ns;
    non_rt_task->real_wcet_ns = execution_ns;
    non_rt_task->wcrt_ns = 0;

    non_rt_task->response_time_ns = (long long*)malloc(non_rt_task->num_samples * sizeof(long long));
    for (int k = 0; k < non_rt_task->num_samples; k++){
        non_rt_task->response_time_ns[k] = 0;
    }
}


int getNumTasks(char *json_file_name){
    json_object *tasks_info_json = json_object_from_file(json_file_name);
    if (tasks_info_json == NULL){
        printf("Error: when read json file\n");
        exit(1);
    }
    json_object *mapping_info = json_object_object_get(tasks_info_json, "mappingInfo");

    int num_cores = json_object_array_length(mapping_info);
    int num_tasks = 0;
    for (int i = 0; i < num_cores; i++){
        json_object *core_info = json_object_array_get_idx(mapping_info, i);
        int core_id = json_object_get_int(json_object_object_get(core_info, "coreID"));
        json_object *tasks_info = json_object_object_get(core_info, "tasks");
        int num_tasks_core = json_object_array_length(tasks_info);
        num_tasks += num_tasks_core;
    }
    return num_tasks;
}

void setNiceAndPriority_2(Task_Info *tasks, int num_tasks, double nice_lambda){
    double min_period_ns = INT_MAX; 
    double max_period_ns = 0;
    for (int i = 0; i < num_tasks; i++){
        if (tasks[i].period_ns < min_period_ns){
            min_period_ns = tasks[i].period_ns;
        }
        if (tasks[i].period_ns > max_period_ns){
            max_period_ns = tasks[i].period_ns;
        }
    }

    for (int i = 0; i < num_tasks; i++){
        /*$nice_i = -19 + \lceil (\frac{D_i -D_{min}}{D_{max} -D_{min}}) \times 39 \rceil$*/
        double ratio = (tasks[i].period_ns - min_period_ns) / (max_period_ns - min_period_ns);
        tasks[i].nice_value = -19 + ceil(ratio * 39);
        tasks[i].priority = 69 - tasks[i].nice_value;
    }
    return;
}

void setNiceAndPriority(Task_Info *tasks, int num_tasks, double nice_lambda){
    double min_period_ns = 0; 
    for (int i = 0; i < num_tasks; i++){
        if (i == 0){
            min_period_ns = tasks[i].period_ns;
        }else{
            if (tasks[i].period_ns < min_period_ns){
                min_period_ns = tasks[i].period_ns;
            }
        }
    }
    for (int i = 0; i < num_tasks; i++){
        tasks[i].nice_value = setNiceValueByDeadline(tasks[i].period_ns, min_period_ns, nice_lambda);
        tasks[i].priority = 69 - tasks[i].nice_value;
    }
    return;
}

int setNiceValueByDeadline( long long period_ns,  long long min_period_ns, double nice_lambda){
    double relative_weight = log((double)period_ns/(double)min_period_ns) * nice_lambda;
    return min(-20 + (int) relative_weight, 19);
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
            long long real_wcet = getRealWCETByName(task_name, tasks, num_tasks)/1000 + 1;
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

    printf("Update real_wcet in %s\n", input_file_name);

    return;
}


long long getRealWCETByName(char* task, Task_Info *tasks, int num_tasks){
    for (int i = 0; i < num_tasks; i++){
        if (strcmp(task, tasks[i].name) == 0){
            return tasks[i].real_wcet_ns;
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
    int count_valid_response_time = 0;
    for (int j = 1; j < task->num_samples; j++) {
        if (task->response_time_ns[j] != 0){
            json_object_array_add(task_response_time_ns, json_object_new_int64(task->response_time_ns[j]));
            total_response_time_ns += task->response_time_ns[j];
            count_valid_response_time++;
            json_object_array_add(task_start_time_ns, json_object_new_int64(task->start_time_ns[j]));
            json_object_array_add(task_end_time_ns, json_object_new_int64(task->end_time_ns[j]));
        }
    }
    long long avg_response_time_ns = 0;
    if (count_valid_response_time != 0){
        avg_response_time_ns= total_response_time_ns / count_valid_response_time;
    }

    long long deadline_ns = 0;
    if (task->isRTTask){
        if (task->isPeriodic){
            deadline_ns = task->period_ns;
        }else{
            deadline_ns = task->low_interarrival_time_ns;
        }
    }else{
        deadline_ns = task->period_ns; 
    }
    json_object_object_add(task_result, "task_name", json_object_new_string(task->name));
    json_object_object_add(task_result, "core_index", json_object_new_int(task->core_index));
    json_object_object_add(task_result, "nice_value", json_object_new_int(task->nice_value));
    json_object_object_add(task_result, "priority", json_object_new_int(task->priority));
    json_object_object_add(task_result, "deadline_ns", json_object_new_int64(deadline_ns));
    json_object_object_add(task_result, "wcrt_ns", json_object_new_int64(task->wcrt_ns));
    json_object_object_add(task_result, "wcet_ns", json_object_new_int64(task->real_wcet_ns));
    json_object_object_add(task_result, "avg_response_time_ns", json_object_new_int64(avg_response_time_ns));
    // json_object_object_add(task_result, "response_time_ns", task_response_time_ns);
    // json_object_object_add(task_result, "start_time_ns", task_start_time_ns);
    // json_object_object_add(task_result, "end_time_ns", task_end_time_ns);
}

void freeTaskInfo(Task_Info *task){
    free(task->response_time_ns);
    return;
}


void setCoreMapping(pthread_attr_t *threadAttr, Task_Info *task){
    if (pthread_attr_init(threadAttr)){
        printf("Fail to initialize thread attribute.\n");
        exit(1);
    }

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
        default:
            printf("Check the supported scheduler type.\n");
            break;
    }
    return;
}