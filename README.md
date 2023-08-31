# CFS Simulator

## Overview

This CFS Simulator program simulates the Completely Fair Scheduling (CFS) policy, which is a Linux scheduling algorithm. The simulator reads input from a `tasks.json` file, runs the simulation, and then outputs the results. The program considers shared resources, nice, and execution time of the task. 

Notion: https://www.notion.so/CFS-e51cd21299a847df926373843dcfe05e

## Features

- Simulates multi-core processors with multiple tasks 
- Uses virtual runtime to select the task  
- Diverge path if multiple blocking tasks or tasks with equal minimum virtual runtime exist
- Outputs Worst-Case Response Times (WCRT) for tasks

## Prerequisites

- Java 8 or higher

## Input

The program reads task information from a `tasks.json` file. Each task has properties such as:

- `id`: Unique identifier of the task
- `startTime`: Time the task starts
- `readTime`: Time taken for read stage
- `bodyTime`: Time taken for body stage
- `writeTime`: Time taken for write stage 
- `nice`: Priority value of the task
- `period`: Period of the task
- `index`: Index of the task in the core 

You can create a sample `tasks.json` file using the included JsonTaskCreator program. 

## Output

The simulation logs include:

- The time-step of the simulation
- Tasks that are released or completed
- Any diverging paths in the simulation
- Final WCRT results for each task