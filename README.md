# WCRT analysis for CFS in Linux systems

## Overview

We provide the Java application specifically designed for task scheduling simulation and Worst-Case Response Time (WCRT) analysis in the context of Linux's Completely Fair Scheduling (CFS). It is important to note that this application is currently under active development and enhancements. 

In the [directory](real_linux_application), you'll find an application developed for direct WCRT measurement by running tasks on actual Linux systems. For a detailed explanation of the implementation, please refer to the Implementation Detail [document](real_linux_application/Implementation_detail.md).

For a comprehensive understanding of the task set and instructions for conducting experiments, please refer to the below.


## Prerequisites

###  To run simulator and analyze WCRT by proposed method
* **Java Version**: Requires Java 8 or higher.
* **Build Tool**: Gradle is used for building the application and managing dependencies.
* Refer to [build.gradle.kts](./app/build.gradle.kts) for detailed dependency information.

###  To run taskset on real Linux systems.
* **GCC Version**: Requires gcc 9.4 or higher.
* **Build Tool**: Cmake is used for building the application and managing dependencies.
* Refer to [CMakeLists.txt](.real_linux_application/app/CMakeLists.txt) for detailed dependency information.
* We tested the application on Raspberry Pi4 which installed Ubuntu 22.04

### Python Libraries (for Additional Analysis Only)
'''
pip3 install pandas matplotlib scikit-learn
'''

## Experiment Instructions

### Generating a Synthetic Dataset
* We provide the complete set of task sets used in our experiments. (Available in the [file](._generated_taskset.zip))
* To generate new task sets, execute the following command. Note that periods are in microseconds.
* You can modify the number of tasks in a set and the system utilization in [main.py](./task_generation/main.py).
* For each combination of task numbers and system utilizations, a corresponding number of task sets (num_tasksets) will be generated.
```
python3 ./task_generation/main.py --generated_files_save_dir=test_gen_tasksets --num_tasksets=100 --min_period=20000 --max_period=1000000
```

### Running the Simulation and the Proposed WCRT Analysis
* Utilize the simulation and the proposed WCRT analysis to evaluate the WCRT for each task set.
```
bash ./script/run/proposed_and_simulator/run_exps.sh
```

### Executing Tasks on a Real Linux System
* Build the application with the following command:
```    
bash ./script/run/real_linux/build_application.sh
```

* To specify a task's nice value from -20 to +19, sudo privileges are required.
* Tasks based on the generated task set information are executed on a real Linux system to measure the actual WCRT.
```    
sudo bash ./script/run/real_linux/run_exps_CFS.sh
```

* To compare the performance of CFS with other RT schedulers with a mixed critical system, use the following command (Refer to Table 5):
```
sudo bash ./script/run/real_linux/run_exps_with_nonRT.sh
```

## Analyze the experimental result

* **Time Consumption Analysis** : [notebook](./script/analysis/time_complexity.ipynb)
* **Comparison(Proposed vs Simulator)** : [notebook](./script/analysis/comparison_with_simulator.ipynb)
* **Comparison(Proposed vs RealLinux)** : [notebook](./script/analysis/comparison_with_realLinux.ipynb)

For more detailed information and instructions, please refer to the script directory in the project.