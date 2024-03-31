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

void setTaskInfo_fmtv(json_object *jobj, Task_Info *task);
void freeTaskInfo(Task_Info *task);
void saveResultToJson(int num_tasks, Task_Info *tasks, Task_Info *non_RT_task, char *result_file_name);
void convertTaskResultToJson(json_object *task_result, Task_Info *task);
int setNiceValueByDeadline( long long period,  long long min_period, double nice_lambda);

void setCoreMapping(pthread_attr_t *threadAttr, Task_Info *task);
// void setSchedPolicyPriority(pthread_attr_t *threadAttr, Task_Info *task);
void printSchedPolicy(int policy);

void initMutex(pthread_mutex_t *mutex_memory_access, int mutex_protocol);
long long getWCETByName(char* task, Task_Info *tasks, int num_tasks);
void updateRealWCET(char* input_file_name, Task_Info *tasks, int num_tasks);