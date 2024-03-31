#include "util.h"

// pthread_barrier_t barrier;
pthread_mutex_t mutex_memory_access = PTHREAD_MUTEX_INITIALIZER;
struct timespec global_start_time = {0, 0};
bool terminate = false;
bool isPhasedTask = false;
double nice_lambda = 3.25;

int main(int argc, char* argv[]){
    if (!(argc == 6 || argc == 7)) {
        printf("Usage: %s <sched_policy> <simulation_period_sec> <input_file_name> <result_file_name> [-non_RT]\n", argv[0]);
        printf("  <sched_policy>: 0 for CFS, 1 for FIFO, 2 for RR, 3 for EDF, 4 for RM\n");
        printf("  <simulation_period_sec>: the period of the simulation in seconds\n");
        printf("  <input_file_name>: the name of the json file that contains the task information\n");
        printf("  <data_type_index>: 0 for fmtv, 1 for uunifest\n");
        printf("  <result_file_name>: the name of the json file to save the result\n");
        printf("  [-non_RT]: exp with a non_RT_Task\n");
        exit(1);
    }

    // parse arguments
    printSchedPolicy(atoi(argv[1]));
    int simulation_period_sec = atoi(argv[2]);
    char *json_file_name = argv[3];
    int data_type_index = atoi(argv[4]);
    if (data_type_index != 0 && data_type_index != 1){
        printf("Error: data_type_index should be 0 or 1\n");
        exit(1);
    }
    char *result_file_name = argv[5];
    
    printf("Read Tasks_info from %s\n", json_file_name);
    json_object *tasks_info_json = json_object_from_file(json_file_name);
    if (tasks_info_json == NULL){
        printf("Error: when read json file\n");
        exit(1);
    }

    if (data_type_index == 1){ //uunifest
        printf("Use the uunifest data type\n");
        json_object *tasks_ids = json_object_object_get(tasks_info_json, "idNameMap");
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

        Task_Info tasks[num_tasks]; //rt_tasks
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
                tasks[task_index].sched_policy = atoi(argv[1]);
                tasks[task_index].isPeriodic = true;
                tasks[task_index].nice_value = json_object_get_int(json_object_object_get(task_info, "nice"));
                tasks[task_index].priority = 69 - tasks[task_index].nice_value;
                tasks[task_index].period_ns = 1000 * json_object_get_int64(json_object_object_get(task_info, "period"));
                tasks[task_index].phased_read_time_ns = 1000 * json_object_get_int64(json_object_object_get(task_info, "readTime"));
                tasks[task_index].phased_write_time_ns = 1000 * json_object_get_int64(json_object_object_get(task_info, "writeTime"));
                tasks[task_index].phased_execution_time_ns = NULL;
                tasks[task_index].body_time_ns = 1000 * json_object_get_int64(json_object_object_get(task_info, "bodyTime"));

                tasks[task_index].num_samples = (simulation_period_sec*1000000) / (tasks[task_index].period_ns/1000); //us
                tasks[task_index].num_runnables = 1;
                tasks[task_index].wcet_ns = tasks[task_index].phased_read_time_ns + tasks[task_index].phased_write_time_ns + tasks[task_index].body_time_ns;

                tasks[task_index].response_time_ns = (long long*)malloc(tasks[task_index].num_samples * sizeof(long long));
                for (int k = 0; k < tasks[task_index].num_samples; k++){
                    tasks[task_index].response_time_ns[k] = 0;
                }
                task_index++;
            }
        }

        // set nice value
        double min_period_ns = 10.0 * 1000.0 * 1000.0 * 1000.0; // 10s
        for (int i = 0; i < num_tasks; i++){
            if (tasks[i].period_ns < min_period_ns){
                min_period_ns = tasks[i].period_ns;
            }
        }
        for (int i = 0; i < num_tasks; i++){
            tasks[i].nice_value = setNiceValueByDeadline(tasks[i].period_ns, min_period_ns, nice_lambda);
        }

        // for test non_rt tasks
        Task_Info non_rt_task;
        if ((argc == 7) && (strcmp(argv[6], "-non_RT") == 0)){
            non_rt_task.name = "Non_RT_Task";
            non_rt_task.core_index = tasks[0].core_index;
            non_rt_task.isRTTask = false;
            non_rt_task.sched_policy = CFS;
            non_rt_task.isPeriodic = false;
            non_rt_task.nice_value = 19;
            non_rt_task.period_ns = 0;
            non_rt_task.phased_read_time_ns = 0;
            non_rt_task.phased_write_time_ns = 0;
            non_rt_task.phased_execution_time_ns = NULL;
            non_rt_task.body_time_ns = 100 * 1000 * 1000; // 100ms
            
            non_rt_task.num_samples = simulation_period_sec * 10; //max: 10 per 1sec
            non_rt_task.num_runnables = 1;
            non_rt_task.wcet_ns = non_rt_task.body_time_ns;

            non_rt_task.response_time_ns = (long long*)malloc(non_rt_task.num_samples * sizeof(long long));
            for (int k = 0; k < non_rt_task.num_samples; k++){
                non_rt_task.response_time_ns[k] = 0;
            }
            printf("Add Non_RT_Task\n");
        }


        printf("Initialize and create tasks\n");
        pthread_attr_t threadAttr[num_tasks];
        pthread_t threads[num_tasks];
        // pthread_barrier_init(&barrier, NULL, num_tasks+1); // to start all threads at the same time
        clock_gettime(CLOCK_MONOTONIC, &global_start_time);
        global_start_time.tv_sec += 3; // wait for all threads to be ready (2sec)
        for (int i = 0; i < num_tasks; i++) {
            setCoreMapping(&threadAttr[i], &tasks[i]); //core mapping
            int ret_pthread_create = 0;
            if (tasks[i].sched_policy == EDF){
                ret_pthread_create = pthread_create(&threads[i], NULL, task_function_unnifest, (void*)&tasks[i]);
            }else{
                ret_pthread_create = pthread_create(&threads[i], &threadAttr[i], task_function_unnifest, (void*)&tasks[i]);
            }
            if (ret_pthread_create){
                printf("Fail to create thread %d\n", i);
                exit(1);
            }
        }

        pthread_attr_t non_RT_threadAttr;
        pthread_t non_RT_threads;
        if ((argc == 7) && (strcmp(argv[6], "-non_RT") == 0)){
            setCoreMapping(&non_RT_threadAttr, &non_rt_task); //core mapping
            if (pthread_create(&non_RT_threads, &non_RT_threadAttr, non_RT_task_function, (void*)&non_rt_task)){
                printf("Fail to create Non_RT thread\n");
                exit(1);
            }
        }

        // clock_gettime(CLOCK_MONOTONIC, &global_start_time);
        // pthread_barrier_wait(&barrier);
        // MARKER("After barrier")
        // printf("global_start_time: %ld.%09ld\n", global_start_time.tv_sec, global_start_time.tv_nsec);
        sleep(3);
        printf("Start to run application.\n The experiment will complete after %d seconds.\n", simulation_period_sec);
        sleep(simulation_period_sec); // seconds

        printf("Terminate tasks\n");
        terminate = true;
        for (int i = 0; i < num_tasks; i++) {
            pthread_join(threads[i], NULL);
        }
        if ((argc == 7) && (strcmp(argv[6], "-non_RT") == 0)){
            pthread_join(non_RT_threads, NULL);
        }
        // for (int i = 0; i < num_tasks; i++) {
        //     if(pthread_cancel(threads[i])){
        //         printf("Fail to cancel thread %d\n", i);
        //         exit(1);
        //     }
        // }
        // pthread_barrier_destroy(&barrier);

        printf("Save the result to %s\n", result_file_name);
        if ((argc == 7) && (strcmp(argv[6], "-non_RT") == 0)){
            saveResultToJson(num_tasks, tasks, &non_rt_task, result_file_name);
        }else{
            saveResultToJson(num_tasks, tasks, NULL, result_file_name);
            updateRealWCET(json_file_name, tasks, num_tasks);
        }
        
        // free memory
        printf("Free Memory of Tasks_info\n");
        for (int i = 0; i < num_tasks; i++){
            freeTaskInfo(&tasks[i]);
        }
        if ((argc == 7) && (strcmp(argv[6], "-non_RT") == 0)){
            freeTaskInfo(&non_rt_task);
        }
        
    }else{ //fmtv(deprecated)
        printf("Use the fmtv data type (deprecated)\n");
    }

    printf("The experiment is complete.\n");
    return 0;
}
