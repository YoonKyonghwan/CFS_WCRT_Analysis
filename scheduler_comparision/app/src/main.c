#include "util.h"

pthread_barrier_t barrier;
pthread_mutex_t mutex_memory_access = PTHREAD_MUTEX_INITIALIZER;
struct timespec global_start_time = {0, 0};
bool terminate = false;
bool isPhasedTask = false;

int main(int argc, char* argv[]){
    if (!(argc == 6 || argc == 7)) {
        printf("Usage: %s <sched_policy> <simulation_period_sec> <input_file_name> <result_file_name> [-phased]\n", argv[0]);
        printf("  <sched_policy>: 0 for CFS, 1 for FIFO, 2 for RR, 3 for EDF, 4 for RM\n");
        printf("  <simulation_period_sec>: the period of the simulation in seconds\n");
        printf("  <input_file_name>: the name of the json file that contains the task information\n");
        printf("  <data_type_index>: 0 for fmtv, 1 for uunifest\n");
        printf("  <result_file_name>: the name of the json file to save the result\n");
        printf("  [-phased]: use the phased task model\n");
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
    if (argc == 7){
        if (strcmp(argv[6], "-phased") == 0){
            isPhasedTask = true;
            printf("Use the phased task model\n");
        }
    }
    
    printf("Read Tasks_info from %s\n", json_file_name);
    json_object *tasks_info_json = json_object_from_file(json_file_name);
    if (tasks_info_json == NULL){
        printf("Error: when read json file\n");
        exit(1);
    }

    if (data_type_index == 0){ //fmtv
        printf("Use the fmtv data type\n");
        int num_tasks = json_object_array_length(tasks_info_json);
        Task_Info tasks[num_tasks];
        for (int i = 0; i < num_tasks; i++){
            json_object *task_info_json = json_object_array_get_idx(tasks_info_json, i);
            setTaskInfo_fmtv(task_info_json, &tasks[i]);
            if(tasks[i].isRTTask){
                tasks[i].sched_policy = atoi(argv[1]);
            }else{
                tasks[i].sched_policy = CFS;
            }
            task_info_json = NULL;
        }
        tasks_info_json = NULL;

        // set nice value
        if (atoi(argv[1]) == CFS){
            for (int i = 0; i < num_tasks; i++){
                if (tasks[i].isPeriodic){
                    tasks[i].nice_value = setNiceValueByDeadline(tasks[i].period_ns);
                }else{
                    tasks[i].nice_value = setNiceValueByDeadline(tasks[i].low_interarrival_time_ns);
                }
            }
        }

        initMutex(&mutex_memory_access, PTHREAD_PRIO_INHERIT); //PTHREAD_PRIO_PROTECT;
        // initMutex(&mutex_memory_access, PTHREAD_PRIO_NONE); //PTHREAD_PRIO_PROTECT;

        printf("Initialize and create tasks\n");
        pthread_attr_t threadAttr[num_tasks];
        pthread_t threads[num_tasks];
        pthread_barrier_init(&barrier, NULL, num_tasks+1); // to start all threads at the same time
        for (int i = 0; i < num_tasks; i++) {
            setCoreMapping(&threadAttr[i], &tasks[i]);
            if (tasks[i].sched_policy == EDF){
                if (pthread_create(&threads[i], NULL, task_function_fmtv, (void*)&tasks[i])){
                    printf("Fail to create thread %d\n", i);
                    exit(1);
                }
            }else{
                if (pthread_create(&threads[i], &threadAttr[i], task_function_fmtv, (void*)&tasks[i])){
                    printf("Fail to create thread %d\n", i);
                    exit(1);
                }
            }
        }
        
        usleep(1000); // wait for all threads to be ready (1ms)
        // clock_gettime(CLOCK_REALTIME, &global_start_time);
        pthread_barrier_wait(&barrier);
        MARKER("After barrier")
        // printf("global_start_time: %ld.%09ld\n", global_start_time.tv_sec, global_start_time.tv_nsec);

        printf("Start to run application.\n The experiment will complete after %d seconds.\n", simulation_period_sec);
        sleep(simulation_period_sec); // seconds

        printf("Terminate tasks\n");
        terminate = true;
        for (int i = 0; i < num_tasks; i++) {
            pthread_join(threads[i], NULL);
        }
        // for (int i = 0; i < num_tasks; i++) {
        //     if(pthread_cancel(threads[i])){
        //         printf("Fail to cancel thread %d\n", i);
        //         exit(1);
        //     }
        // }
        pthread_barrier_destroy(&barrier);


        printf("Save the result to %s\n", result_file_name);
        saveResultToJson(num_tasks, tasks, result_file_name);

        // free memory
        printf("Free Memory of Tasks_info\n");
        for (int i = 0; i < num_tasks; i++){
            freeTaskInfo(&tasks[i]);
        }
    }else{ //uunifest
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

        Task_Info tasks[num_tasks];
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
                tasks[task_index].period_ns = 1000 * json_object_get_int(json_object_object_get(task_info, "period"));
                tasks[task_index].phased_read_time_ns = 1000 * json_object_get_int(json_object_object_get(task_info, "readTime"));
                tasks[task_index].phased_write_time_ns = 1000 * json_object_get_int(json_object_object_get(task_info, "writeTime"));
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
        // for (int i=0 ; i < num_tasks; i++){
        //     printf("Task %d: %s, core_index: %d, sched_policy: %d, nice_value: %d, isRTTask: %d, isPeriodic: %d, period_ns: %lld, phased_read_time_ns: %d, phased_write_time_ns: %d, body_time_ns: %lld, num_samples: %d, num_runnables: %d, wcet_ns: %lld\n", i, tasks[i].name, tasks[i].core_index, tasks[i].sched_policy, tasks[i].nice_value, tasks[i].isRTTask, tasks[i].isPeriodic, tasks[i].period_ns, tasks[i].phased_read_time_ns, tasks[i].phased_write_time_ns, tasks[i].body_time_ns, tasks[i].num_samples, tasks[i].num_runnables, tasks[i].wcet_ns);
        // }

        printf("Initialize and create tasks\n");
        pthread_attr_t threadAttr[num_tasks];
        pthread_t threads[num_tasks];
        pthread_barrier_init(&barrier, NULL, num_tasks+1); // to start all threads at the same time
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

        usleep(1000); // wait for all threads to be ready (1ms)
        // clock_gettime(CLOCK_REALTIME, &global_start_time);
        pthread_barrier_wait(&barrier);
        MARKER("After barrier")
        // printf("global_start_time: %ld.%09ld\n", global_start_time.tv_sec, global_start_time.tv_nsec);

        printf("Start to run application.\n The experiment will complete after %d seconds.\n", simulation_period_sec);
        sleep(simulation_period_sec); // seconds

        printf("Terminate tasks\n");
        terminate = true;
        for (int i = 0; i < num_tasks; i++) {
            pthread_join(threads[i], NULL);
        }
        // for (int i = 0; i < num_tasks; i++) {
        //     if(pthread_cancel(threads[i])){
        //         printf("Fail to cancel thread %d\n", i);
        //         exit(1);
        //     }
        // }
        pthread_barrier_destroy(&barrier);


        printf("Save the result to %s\n", result_file_name);
        saveResultToJson(num_tasks, tasks, result_file_name);
        updateRealWCET(tasks_info_json, tasks, num_tasks, "update_real_wcet.json");
        


        // free memory
        printf("Free Memory of Tasks_info\n");
        for (int i = 0; i < num_tasks; i++){
            freeTaskInfo(&tasks[i]);
        }
        
    }

    printf("The experiment is complete.\n");
    return 0;
}
