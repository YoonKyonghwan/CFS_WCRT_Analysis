#include "util.h"

short simulation_period_sec = 3; // seconds
bool isPhasedTask = false;

pthread_barrier_t barrier;
pthread_mutex_t mutex_memory_access = PTHREAD_MUTEX_INITIALIZER;

int main(int argc, char* argv[]){
    if (argc != 4){
        printf("Usage: %s <sched_type> <tasks_info.json> <result.json>\n", argv[0]);
        exit(1);
    }

    // parse arguments
    printSchedPolicy(atoi(argv[1]));
    char *json_file_name = argv[2];
    char *result_file_name = argv[3];
    if (argv[4] == "-phased"){
        printf("Use the phased task model\n");
        isPhasedTask = true;
    }
    
    printf("Read Tasks_info from %s\n", json_file_name);
    json_object *tasks_info_json = json_object_from_file(json_file_name);
    if (tasks_info_json == NULL){
        printf("Error: json file not found\n");
        exit(1);
    }

    int num_tasks = json_object_array_length(tasks_info_json);
    Task_Info tasks[num_tasks];
    for (int i = 0; i < num_tasks; i++){
        json_object *task_info_json = json_object_array_get_idx(tasks_info_json, i);
        setTaskInfo(task_info_json, &tasks[i]);
        if(tasks[i].isRTTask){
            tasks[i].sched_policy = atoi(argv[1]);
        }else{
            tasks[i].sched_policy = CFS;
        }
        task_info_json = NULL;
    }
    tasks_info_json = NULL;

    printf("Initialize and create tasks\n");
    // set core mapping and scheduler policy
    // set priority of RT task in "task.c"
    pthread_attr_t threadAttr[num_tasks];
    pthread_t threads[num_tasks];
    pthread_barrier_init(&barrier, NULL, num_tasks+1); // to start all threads at the same time
    for (int i = 0; i < num_tasks; i++) {
        setTaskAttribute(&threadAttr[i], &tasks[i]);
        if (pthread_create(&threads[i], &threadAttr[i], task_function, (void*)&tasks[i])){
            printf("Fail to create thread %d\n", i);
            exit(1);
        }
        printf(" (Init) %s\n", tasks[i].name);
    }
    pthread_barrier_wait(&barrier);

    printf("Start to run application.\n The experiment will complete after %d seconds.\n", simulation_period_sec);
    usleep(simulation_period_sec * 1000000); // seconds to microseconds

    printf("Terminate tasks\n");
    for (int i = 0; i < num_tasks; i++) {
        pthread_cancel(threads[i]);
    }
    pthread_barrier_destroy(&barrier);

    printf("Save the result to %s\n", result_file_name);
    saveResultToJson(num_tasks, tasks, result_file_name);

    for (int i = 0; i < num_tasks; i++){
        freeTaskInfo(&tasks[i]);
    }
    
    printf("The experiment is complete.\n\n\n");
    return 0;
}
