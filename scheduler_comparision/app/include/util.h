#define _GNU_SOURCE
#pragma once
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <pthread.h>
#include <json-c/json.h>
#include <math.h>
#include "task.h"

// scheduler type
enum SCHEDULER_TYPE {
    CFS, 
    FIFO,
    RR,
    EDF, 
    RM
};

void setTaskInfo(char *json_file_name, Task_Info *tasks, int sched_policy, int simulation_period_sec);
void setNonRTTaskInfo(Task_Info* non_rt_task, char* name, int core_index, int execution_ns, int period_ns, int num_samples);

void setNiceAndPriority(Task_Info *tasks, int num_tasks, double nice_lambda);
int setNiceValueByDeadline( long long period,  long long min_period, double nice_lambda);
int getNumTasks(char *json_file_name);

void freeTaskInfo(Task_Info *task);

void saveResultToJson(int num_tasks, Task_Info *tasks, Task_Info *non_RT_task, char *result_file_name);
void convertTaskResultToJson(json_object *task_result, Task_Info *task);

void setCoreMapping(pthread_attr_t *threadAttr, Task_Info *task);
// void setSchedPolicyPriority(pthread_attr_t *threadAttr, Task_Info *task);
void printSchedPolicy(int policy);

long long getWCETByName(char* task, Task_Info *tasks, int num_tasks);
void updateRealWCET(char* input_file_name, Task_Info *tasks, int num_tasks);