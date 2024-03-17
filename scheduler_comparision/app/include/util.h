#define _GNU_SOURCE
#pragma once
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <pthread.h>
#include <json-c/json.h>
#include "task.h"

// scheduler type
enum SCHEDULER_TYPE {
    CFS, 
    FIFO,
    RR,
    EDF, 
    RM
};

void setTaskInfo(json_object *jobj, Task_Info *task);
void freeTaskInfo(Task_Info *task);
void saveResultToJson(int num_tasks, Task_Info *tasks, char *result_file_name);
long long setNiceValueByDeadline(long long period);

void setTaskAttribute(pthread_attr_t *threadAttr, Task_Info *task);
void setCoreMapping(Task_Info *task, pthread_attr_t *threadAttr);
// void setSchedPolicyPriority(pthread_attr_t *threadAttr, Task_Info *task);
void printSchedPolicy(int policy);

void initMutex(pthread_mutex_t *mutex_memory_access, int mutex_protocol);

