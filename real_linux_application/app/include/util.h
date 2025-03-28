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

long long gcd(long long a, long long b);
long long lcm(long long a, long long b);
long long getHyperperiod_ns(Task_Info *tasks, int num_tasks);

void initTaskInfo(char *json_file_name, Task_Info *tasks, int sched_policy);

void setPriorityByRM(Task_Info *tasks, int num_tasks);
int getNumTasks(char *json_file_name);

void freeTaskInfo(Task_Info *task);

void saveResultToJson(int num_tasks, Task_Info *tasks, Task_Info *non_RT_task, char *result_file_name);
void convertTaskResultToJson(json_object *task_result, Task_Info *task);

void setCoreMapping(pthread_attr_t *threadAttr, Task_Info *task);
void printSchedPolicy(int policy);

long long getRealWCETByName(char* task, Task_Info *tasks, int num_tasks);
void updateRealWCET(char* input_file_name, Task_Info *tasks, int num_tasks);