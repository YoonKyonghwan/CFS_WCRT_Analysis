#include "util.h"


struct timespec global_start_time = {0, 0};
bool terminate = false;
// bool initial_try = true;
double nice_lambda = 20;
int wait_initialization_sec = 2;

int main(int argc, char* argv[]){
    if (!(argc == 5)) {
        printf("Usage: ./application <sched_policy> <num_repeat> <input_file_name> <result_file_name> \n");
        printf("  <sched_policy>: 0 for CFS, 1 for FIFO, 2 for RR, 3 for EDF, 4 for RM\n");
        printf("  <num_repeat>: the number of repeat\n");
        printf("  <input_file_name>: the name of the json file that contains the task information\n");
        printf("  <result_file_name>: the name of the json file to save the result\n");
        exit(1);
    }

    // parse arguments
    printSchedPolicy(atoi(argv[1]));
    int num_repeat = atoi(argv[2]);
    char *json_file_name = argv[3];
    char *result_file_name = argv[4];
    int num_tasks = getNumTasks(json_file_name);  

    printf("Read Tasks_info from %s\n", json_file_name);
    Task_Info tasks_info[num_tasks]; //rt_tasks
    setTaskInfo(json_file_name, tasks_info, atoi(argv[1]));
    long simulation_period_us = (long) (2 * getHyperperiod_ns(tasks_info, num_tasks)) / 1000;

    // set nice value
    printf("Set nice values and priorities.\n");
    // setNiceAndPriority(tasks_info, num_tasks, nice_lambda);
    setNiceAndPriority2(tasks_info, num_tasks);

    // print task information
    printf("Task Information\n");
    for (int i = 0; i < num_tasks; i++){
        printf("    %s (RT %d, priority %d, nice %d, sched_policy %d)\n", tasks_info[i].name, tasks_info[i].isRTTask, tasks_info[i].priority, tasks_info[i].nice_value, tasks_info[i].sched_policy);
    }

    printf("num_repeat: %d\n", num_repeat);
    for(int repeat_index = 0; repeat_index < num_repeat; repeat_index++){
        printf("Repeat_index %d\n", repeat_index);
        printf("    Initialize and create tasks\n");
        terminate = false;
        // if (repeat_index != 0)  initial_try = false;
        pthread_attr_t threadAttr[num_tasks];
        pthread_t rt_threads[num_tasks];
        clock_gettime(CLOCK_MONOTONIC, &global_start_time);
        global_start_time.tv_sec += wait_initialization_sec; // wait for all threads to be ready (3sec)
        // printf("global_start_time: %ld sec %ld ns\n", global_start_time.tv_sec, global_start_time.tv_nsec);
        for (int i = 0; i < num_tasks; i++) {
            setCoreMapping(&threadAttr[i], &tasks_info[i]); //core mapping
            int ret_pthread_create = 0;
            if (tasks_info[i].sched_policy != EDF){
                ret_pthread_create = pthread_create(&rt_threads[i], &threadAttr[i], task_function, (void*)&tasks_info[i]);
            }else{
                ret_pthread_create = pthread_create(&rt_threads[i], NULL, task_function, (void*)&tasks_info[i]);
            }
            if (ret_pthread_create){
                printf("Fail to create thread %d\n", i);
                exit(1);
            }
        }

        sleep(wait_initialization_sec);
        printf("    Start to run application. (simulation time : %ld us)\n", simulation_period_us);
        usleep(simulation_period_us);

        printf("    Terminate tasks.\n");
        terminate = true;
        for (int i = 0; i < num_tasks; i++) {
            pthread_join(rt_threads[i], NULL);
        }

        usleep(10000);
    }

    printf("Save the result to %s\n", result_file_name);
    saveResultToJson(num_tasks, tasks_info, NULL, result_file_name);
    // updateRealWCET(json_file_name, tasks_info, num_tasks);
    
    // free memory
    printf("Free Memory of Tasks_info\n");
    for (int i = 0; i < num_tasks; i++){
        freeTaskInfo(&tasks_info[i]);
    }

    printf("The experiment is complete.\n");
    return 0;
}
