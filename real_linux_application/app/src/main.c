#include "util.h"


struct timespec global_start_time = {0, 0};
bool terminate = false;
double nice_lambda = 3.25;

int main(int argc, char* argv[]){
    if (!(argc == 5) && !(argc == 6)) {
        printf("Usage: ./application <sched_policy> <simulation_period_sec> <input_file_name> <result_file_name> [-non_RT]\n");
        printf("  <sched_policy>: 0 for CFS, 1 for FIFO, 2 for RR, 3 for EDF, 4 for RM\n");
        printf("  <simulation_period_sec>: the period of the simulation in seconds\n");
        printf("  <input_file_name>: the name of the json file that contains the task information\n");
        printf("  <result_file_name>: the name of the json file to save the result\n");
        printf("  [-non_RT]: exp with a non_RT_Task\n");
        exit(1);
    }

    // parse arguments
    printSchedPolicy(atoi(argv[1]));
    int simulation_period_sec = atoi(argv[2]);
    char *json_file_name = argv[3];
    char *result_file_name = argv[4];
    int num_tasks = getNumTasks(json_file_name);  
    bool isMixedCritical = false;
    if ((argc == 6) && (strcmp(argv[5], "-non_RT") == 0)){
        isMixedCritical = true;
        printf("The experiment is a mixed critical system.\n");
    }  
    

    printf("Read Tasks_info from %s\n", json_file_name);
    Task_Info rt_tasks_info[num_tasks]; //rt_tasks
    setTaskInfo(json_file_name, rt_tasks_info, atoi(argv[1]), simulation_period_sec);

    // set nice value
    printf("Set nice values and priorities.\n");
    setNiceAndPriority(rt_tasks_info, num_tasks, nice_lambda);

    // for experiment of a mixed critical system
    Task_Info nrt_task_info;
    if (isMixedCritical){
        int core_index = rt_tasks_info[0].core_index;
        long long execution_ns = 36 * 1000 * 1000;
        long long period_ns = 300 * 1000 * 1000;
        int num_samples = (simulation_period_sec*1000) / (period_ns/1000000);
        setNonRTTaskInfo(&nrt_task_info, "Non_RT_Task", core_index, execution_ns, period_ns, num_samples);
    }

    printf("Initialize and create tasks\n");
    pthread_attr_t threadAttr[num_tasks];
    pthread_t rt_threads[num_tasks];
    clock_gettime(CLOCK_MONOTONIC, &global_start_time);
    global_start_time.tv_sec += 3; // wait for all threads to be ready (3sec)
    for (int i = 0; i < num_tasks; i++) {
        setCoreMapping(&threadAttr[i], &rt_tasks_info[i]); //core mapping
        int ret_pthread_create = 0;
        if (rt_tasks_info[i].sched_policy != EDF){
            ret_pthread_create = pthread_create(&rt_threads[i], &threadAttr[i], task_function, (void*)&rt_tasks_info[i]);
        }else{
            ret_pthread_create = pthread_create(&rt_threads[i], NULL, task_function, (void*)&rt_tasks_info[i]);
        }
        if (ret_pthread_create){
            printf("Fail to create thread %d\n", i);
            exit(1);
        }
    }

    // for experiment of a mixed critical system
    pthread_attr_t nrt_threadAttr;
    pthread_t nrt_thread;
    if (isMixedCritical){
        setCoreMapping(&nrt_threadAttr, &nrt_task_info); //core mapping
        if (pthread_create(&nrt_thread, &nrt_threadAttr, task_function, (void*)&nrt_task_info)){
            printf("Fail to create Non_RT thread\n");
            exit(1);
        }
    }

    sleep(3);
    printf("Start to run application.\n The experiment will complete after %d seconds.\n", simulation_period_sec);
    sleep(simulation_period_sec); // seconds

    printf("Terminate tasks\n");
    terminate = true;
    for (int i = 0; i < num_tasks; i++) {
        pthread_join(rt_threads[i], NULL);
    }
    if (isMixedCritical){
        pthread_join(nrt_thread, NULL);
    }

    printf("Save the result to %s\n", result_file_name);
    if (isMixedCritical){
        saveResultToJson(num_tasks, rt_tasks_info, &nrt_task_info, result_file_name);
    }else{
        saveResultToJson(num_tasks, rt_tasks_info, NULL, result_file_name);
        updateRealWCET(json_file_name, rt_tasks_info, num_tasks);
    }
    
    // free memory
    printf("Free Memory of Tasks_info\n");
    for (int i = 0; i < num_tasks; i++){
        freeTaskInfo(&rt_tasks_info[i]);
    }
    if ((argc == 7) && (strcmp(argv[6], "-non_RT") == 0)){
        freeTaskInfo(&nrt_task_info);
    }

    printf("The experiment is complete.\n");
    return 0;
}
