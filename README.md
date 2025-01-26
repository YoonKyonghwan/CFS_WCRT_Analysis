# WCRT analysis for CFS in Linux systems

## Overview

We provide the Java application specifically designed for task scheduling simulation and Worst-Case Response Time (WCRT) analysis in the context of Linux's Completely Fair Scheduling (CFS). It is important to note that this application is currently under active development and enhancements. 

In the [real_linux_application](real_linux_application), you'll find an application developed for direct WCRT measurement by running tasks on actual Linux systems. For a detailed explanation of the implementation, please refer to the [document](real_linux_application/Description.md).

For a comprehensive understanding of the task set and instructions for conducting experiments, please refer to the below.


## Prerequisites

###  Clone the repository
    git clone --recurse-submodules [repository address]

###  To run simulator and analyze WCRT by proposed method
* **Java Version**: Requires Java 8 or higher.
* **Build Tool**: Gradle is used for building the application and managing dependencies.
* Refer to [build.gradle.kts](./app/build.gradle.kts) for detailed dependency information.

###  To run taskset on real Linux systems.
* **GCC Version**: Requires gcc 9.4 or higher.
* **Build Tool**: Cmake is used for building the application and managing dependencies.
* Refer to [CMakeLists.txt](.real_linux_application/app/CMakeLists.txt) for detailed dependency information.
* We tested the application on Raspberry Pi4 which installed Rasbian(Linux Kernel 5.15.92)

### Python Libraries (for Additional Analysis Only)
    pip3 install pandas matplotlib scikit-learn



## Experiment Instructions

### Generating a Synthetic Dataset
* We provide the complete set of task sets used in our experiments. (Available in the [file](._generated_taskset.tar))
* To generate new task sets, execute the following command. Note that periods are in microseconds.
* You can modify the number of tasks in a set and the system utilization in [main.py](./task_generation/main.py).
* For each combination of task numbers and system utilizations, a corresponding number of task sets (num_tasksets) will be generated.
    ```
    python3 ./task_generation/main.py --generated_files_save_dir=test_gen_tasksets --num_tasksets=100 --min_period=30000 --max_period=3000000
    ```

### To Evaluate the Schedulability by the Simulator & the Proposed Analysis
1. **Build the application**
    ```bash
    gradle wrapper
    ./gradlew build
    ```
2. **Configure the experiment settings in the [script](./script/run/proposed_and_simulator/run_exps.sh)**:
    * **num_tasks**: List of the number of tasks (default: 2, 4, 6, 8, 10)
    * **utilization**: List of utilization values (default: 0.2, 0.4, 0.6, 0.8)
    * **target_latency**: Target latency in microseconds (default: 18,000)
    * **min_gran**: Minimum granularity in microseconds (default: 2,250)
    * **jiffy**: Jiffy value in microseconds (default: 1,000)
    * **nice_assign**: Strategy for assigning nice values (default: GA)
3. **Run the script**:
    ```bash
    bash ./script/run/proposed_and_simulator/run_exps.sh
    ```
    * (Optional) To save experiment time, you can run the program in parallel based on the combination of the number of tasks and utilization settings. Make sure to configure the settings before running the [parallel script](./script/run/proposed_and_simulator/run_exps_parallel.sh).
        ```bash
        bash ./script/run/proposed_and_simulator/run_exps_parallel.sh
        ```

### To Evaluate the Schedulability on a Real Linux System
1. **Build the application**:
    ```bash
    bash ./script/run/real_linux/build_application.sh
    ```
2. **Configure the experiment settings in the [script](./script/run/real_linux/run_exps_with_synthetic_tasksets.sh)**:
    * **num_tasks**: List of the number of tasks (default: 2, 4, 6, 8, 10)
    * **utilization**: List of utilization values (default: 0.2, 0.4, 0.6, 0.8)
3. **Run the application with synthetic task sets**:
    ```bash
    sudo bash ./script/run/real_linux/run_exps_with_synthetic_tasksets.sh
    ```
    * To specify a task's nice value from -20 to +19, sudo privileges are required.

### To Compare CFS with Other Real-time Schedulers on a Real Linux System
1. **Build the application**:
    ```bash
    bash ./script/run/real_linux/build_application.sh
    ```
2. **Run the application with a real-world application**:
    ```bash
    sudo bash ./script/run/real_linux/run_exp_with_real_application.sh
    ```
    * The task set originates from satellite onboard software supplied by Thales Alenia Space (TAS), as outlined in the [paper](https://drops.dagstuhl.de/entities/document/10.4230/LIPIcs.ECRTS.2017.17).


## Analysis of Experimental Results

* **Nice Value Assignment** : [notebook](./script/analysis/comparison_nice_value_assignment.ipynb)
    * This analysis compares the schedulability estimated from the proposed WCRT analysis method using different nice value assignment strategies.
    * It also examines the time consupmtion for analysis depending on the nice value strategies.
* **Overestimation of the Proposed WCRT Analysis** : [notebook](script/analysis/comparison_overestimation.ipynb)
    * This evaluates the schedulability estimated from the proposed WCRT analysis method against those obtained from the simulator and a real Linux system.
* **Suitability of CFS** : [notebook](script/analysis/comparison_task_periods.ipynb)
    * This analysis assesses the schedulability of task sets under the CFS scheduler with varying task periodicities.
* **Comparison of Schedulers** : [notebook](script/analysis/comparison_schedulers.ipynb)
    * This provides a comparison of worst-case and average response times using CFS and other real-time schedulers.

For more detailed information and instructions, please refer to the script directory in the project.